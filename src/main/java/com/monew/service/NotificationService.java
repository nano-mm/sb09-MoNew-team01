package com.monew.service;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.entity.enums.ResourceType;
import com.monew.dto.request.NotificationCreateCommand;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

  CursorPageResponseDto<NotificationDto> getNotifications(UUID userId, String cursor, Instant after, int size);

  void confirmNotification(UUID userId, UUID notificationId);

  void confirmAllNotifications(UUID userId);

  void createNotification(UUID userId, String content, ResourceType resourceType, UUID resourceId);

   int createNotifications(List<NotificationCreateCommand> commands);

   long deleteOldConfirmedNotifications();
}