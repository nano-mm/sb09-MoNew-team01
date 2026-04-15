package com.monew.service;

import com.monew.dto.response.CursorPageResponse;
import com.monew.dto.response.NotificationDto;
import java.util.UUID;

public interface NotificationService {

  CursorPageResponse<NotificationDto> getNotifications(UUID userId, UUID cursorId, int size);
}