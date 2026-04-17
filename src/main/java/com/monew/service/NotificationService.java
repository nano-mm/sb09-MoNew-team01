package com.monew.service;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import java.time.Instant;
import java.util.UUID;

public interface NotificationService {

  CursorPageResponseDto<NotificationDto> getNotifications(UUID userId, String cursor, Instant after, int size);

  void confirmNotification(UUID userId, UUID notificationId);

  void confirmAllNotifications(UUID userId);
}