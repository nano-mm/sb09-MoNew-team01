package com.monew.application.port.in;

import com.monew.domain.model.enums.ResourceType;
import com.monew.dto.request.NotificationCreateCommand;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationUseCase {
  long deleteOldConfirmedNotifications();
  CursorPageResponseDto<NotificationDto> getNotifications(UUID userId, String cursor, Instant after, int limit);
  void confirmNotification(UUID userId, UUID notificationId);
  void confirmAllNotifications(UUID userId);
  void createNotification(UUID userId, String content, ResourceType resourceType, UUID resourceId);
  int createNotifications(List<NotificationCreateCommand> commands);
}
