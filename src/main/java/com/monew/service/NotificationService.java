package com.monew.service;

import java.util.UUID;

public interface NotificationService {

  void getNotifications(UUID userId, UUID cursorId, int size);
}