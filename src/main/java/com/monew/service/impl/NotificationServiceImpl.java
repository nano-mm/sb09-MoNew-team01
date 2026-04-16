package com.monew.service.impl;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.repository.NotificationRepository;
import com.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;

  @Override
  public CursorPageResponseDto<NotificationDto> getNotifications(
      UUID userId,
      String cursor,
      Instant after,
      int size
  ) {
    int pageSize = Math.max(1, size);
    CursorPoint cursorPoint = resolveCursorPoint(userId, cursor, after);

    List<Notification> notifications = notificationRepository.findByUserIdWithCursor(
        userId,
        cursorPoint.after(),
        cursorPoint.cursorId(),
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

    if (!pageItems.isEmpty()) {
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

  private CursorPoint resolveCursorPoint(UUID userId, String cursor, Instant after) {
    if (cursor != null && !cursor.isBlank()) {
      UUID cursorId;
      try {
        cursorId = UUID.fromString(cursor);
      } catch (IllegalArgumentException e) {
        throw new BaseException(ErrorCode.INVALID_INPUT);
      }

      Instant cursorAfter = notificationRepository.findByIdAndUserIdAndConfirmedFalse(cursorId, userId)
          .map(Notification::getCreatedAt)
          .orElseThrow(() -> new BaseException(ErrorCode.INVALID_INPUT));

      return new CursorPoint(cursorAfter, cursorId);
    }

    return new CursorPoint(after, null);
  }

  private record CursorPoint(Instant after, UUID cursorId) {
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