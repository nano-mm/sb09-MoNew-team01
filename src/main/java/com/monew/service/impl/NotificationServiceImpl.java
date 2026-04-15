package com.monew.service.impl;

import com.monew.dto.response.CursorPageResponse;
import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.repository.NotificationRepository;
import com.monew.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;

  @Override
  public CursorPageResponse<NotificationDto> getNotifications(UUID userId, UUID cursorId, int size) {
    List<Notification> notifications = notificationRepository.findByUserIdWithCursor(userId, cursorId, size);

    List<NotificationDto> content = notifications.stream()
        .map(this::toDto)
        .toList();

    boolean hasNext = content.size() == Math.max(1, size);
    String nextCursor = null;
    java.time.Instant nextAfter = null;

    if (hasNext && !notifications.isEmpty()) {
      Notification last = notifications.get(notifications.size() - 1);
      nextCursor = last.getId().toString();
      nextAfter = last.getCreatedAt();
    }

    return new CursorPageResponse<>(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        notificationRepository.countByUserId(userId),
        hasNext
    );
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