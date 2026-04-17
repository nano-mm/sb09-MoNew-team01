package com.monew.service.impl;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import com.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @Override
  public CursorPageResponseDto<NotificationDto> getNotifications(
      UUID userId,
      String cursor,
      Instant after,
      int size
  ) {
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
        .map(this::toDto)
        .toList();

    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext && !pageItems.isEmpty()) {
      Notification last = pageItems.get(pageItems.size() - 1);
      nextCursor = last.getId().toString();
      nextAfter = last.getCreatedAt();
    }

    return new CursorPageResponseDto<>(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        notificationRepository.countByUserIdAndConfirmedFalse(userId),
        hasNext
    );
  }

  @Override
  @Transactional
  public void confirmNotification(UUID userId, UUID notificationId) {
    assertUserExists(userId);
    notificationRepository.findByIdAndUserIdAndConfirmedFalse(notificationId, userId)
        .ifPresent(Notification::confirm);
  }

  @Override
  @Transactional
  public void confirmAllNotifications(UUID userId) {
    assertUserExists(userId);
    List<Notification> notifications = notificationRepository.findAllByUserIdAndConfirmedFalse(userId);

    if (notifications.isEmpty()) {
      return;
    }

    notifications.forEach(Notification::confirm);
    notificationRepository.saveAll(notifications);
  }

  private void assertUserExists(UUID userId) {
    userRepository.findById(userId)
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

      return notificationRepository.findByIdAndUserIdAndConfirmedFalse(cursorId, userId)
          .map(Notification::getCreatedAt)
          .orElseThrow(() -> new BaseException(ErrorCode.INVALID_INPUT));
    }

    return after;
  }

  private NotificationDto toDto(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getCreatedAt(),
        notification.getUpdatedAt(),
        Boolean.TRUE.equals(notification.getConfirmed()),
        notification.getUserId(),
        notification.getContent(),
        notification.getResourceType().name().toLowerCase(),
        notification.getResourceId()
    );
  }
}