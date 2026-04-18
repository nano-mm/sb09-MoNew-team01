package com.monew.service.impl;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.NotificationMapper;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import com.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationMapper notificationMapper;

  @Override
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

  @Override
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

  @Override
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

  private void assertUserExists(UUID userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    log.debug("[알림] 사용자 검증 완료. userId={}", userId);
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