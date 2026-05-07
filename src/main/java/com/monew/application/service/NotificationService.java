package com.monew.application.service;
import java.time.temporal.ChronoUnit;

import com.monew.application.port.in.NotificationUseCase;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.dto.request.NotificationCreateCommand;
import com.monew.domain.model.Notification;
import com.monew.domain.model.User;
import com.monew.domain.model.enums.ResourceType;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.NotificationMapper;
import com.monew.application.port.out.persistence.NotificationRepository;
import com.monew.application.port.out.persistence.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationUseCase {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationMapper notificationMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public long deleteOldConfirmedNotifications() {
    Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
    long deleted = notificationRepository.deleteByConfirmedIsTrueAndCreatedAtBefore(threshold);
    log.info("[알림] 1주일 경과 확인 알림 삭제. 삭제 건수={}", deleted);
    return deleted;
  }

  public CursorPageResponseDto<NotificationDto> getNotifications(
      UUID userId,
      String cursor,
      Instant after,
      int size
  ) {
    log.debug(
        "[알림] 조회 처리 시작. userId={}, 요청크기={}, cursor존재={}, after존재={}",
        userId,
        size,
        cursor != null && !cursor.isBlank(),
        after != null
    );

    assertUserExists(userId);

    int pageSize = Math.max(1, size);
    Instant cursorPoint = resolveCursorPoint(userId, cursor, after);

    List<Notification> notifications = notificationRepository.findByUserIdWithCursor(
        userId,
        cursorPoint,
        pageSize + 1
    );

    boolean hasNext = notifications.size() > pageSize;
    List<Notification> pageItems = hasNext
        ? notifications.subList(0, pageSize)
        : notifications;

    List<NotificationDto> content = pageItems.stream()
        .map(notificationMapper::toDto)
        .toList();

    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext && !pageItems.isEmpty()) {
      Notification last = pageItems.get(pageItems.size() - 1);
      nextCursor = last.getId().toString();
      nextAfter = last.getCreatedAt();
    }

    long totalElements = notificationRepository.countByUser_IdAndConfirmedFalse(userId);
    CursorPageResponseDto<NotificationDto> response = new CursorPageResponseDto<>(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext
    );

    log.debug(
        "[알림] 조회 처리 완료. userId={}, 반환개수={}, 전체개수={}, 다음페이지존재={}",
        userId,
        response.size(),
        response.totalElements(),
        response.hasNext()
    );

    return response;
  }

  @Transactional
  public void confirmNotification(UUID userId, UUID notificationId) {
    assertUserExists(userId);
    boolean confirmed = notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notificationId, userId)
        .map(notification -> {
          notification.confirm();
          return true;
        })
        .orElse(false);

    if (confirmed) {
      log.info("[알림] 단건 확인 반영. userId={}, notificationId={}", userId, notificationId);
      return;
    }

    log.debug(
        "[알림] 단건 확인 미반영. userId={}, notificationId={}, 사유=없거나_이미_확인됨",
        userId,
        notificationId
    );
  }

  @Transactional
  public void confirmAllNotifications(UUID userId) {
    assertUserExists(userId);
    List<Notification> notifications = notificationRepository.findAllByUser_IdAndConfirmedFalse(userId);

    if (notifications.isEmpty()) {
      log.debug("[알림] 전체 확인 미반영. userId={}, 사유=미확인_알림_없음", userId);
      return;
    }

    notifications.forEach(Notification::confirm);
    log.info("[알림] 전체 확인 반영. userId={}, 반영건수={}", userId, notifications.size());
  }

  @Transactional
  public void createNotification(UUID userId, String content, ResourceType resourceType, UUID resourceId) {
    if (userId == null || content == null || content.isBlank() || resourceType == null || resourceId == null) {
        throw new BaseException(ErrorCode.INVALID_INPUT);
    }

    getUserOrThrow(userId);
    // publish as Object to ensure publishEvent(Object) overload is called
    eventPublisher.publishEvent((Object) new com.monew.event.NotificationCreatedEvent(userId, content, resourceType, resourceId));
    log.info("[알림] 단건 생성 이벤트 발행. userId={}, resourceType={}, resourceId={}", userId, resourceType, resourceId);
  }

  @Transactional
  public int createNotifications(List<NotificationCreateCommand> commands) {
    if (commands == null || commands.isEmpty()) {
      log.debug("[알림] 다건 생성 미반영. 사유=요청_비어있음");
      return 0;
    }

    Map<UUID, User> userCache = new HashMap<>();
    int count = 0;
    for (var command : commands) {
      if (command == null) continue;
      userCache.computeIfAbsent(command.userId(), this::getUserOrThrow);
      // publish simple record event for each command
      // publish as Object to consistently call publishEvent(Object)
      eventPublisher.publishEvent((Object) new com.monew.event.NotificationCreatedEvent(command.userId(), command.content(), command.resourceType(), command.resourceId()));
      count++;
    }
    log.info("[알림] 다건 생성 이벤트 발행. 요청건수={}, 발행건수={}", commands.size(), count);
    return count;
  }

  private void assertUserExists(UUID userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    log.debug("[알림] 사용자 검증 완료. userId={}", userId);
  }

  private User getUserOrThrow(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
  }

  private Instant resolveCursorPoint(UUID userId, String cursor, Instant after) {
    if (cursor != null && !cursor.isBlank()) {
      UUID cursorId;
      try {
        cursorId = UUID.fromString(cursor);
      } catch (IllegalArgumentException e) {
        throw new BaseException(ErrorCode.INVALID_INPUT);
      }

      return notificationRepository.findByIdAndUser_IdAndConfirmedFalse(cursorId, userId)
          .map(Notification::getCreatedAt)
          .orElseThrow(() -> new BaseException(ErrorCode.INVALID_INPUT));
    }

    return after;
  }

}