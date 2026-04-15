package com.monew.service.impl;

import com.monew.repository.NotificationRepository;
import com.monew.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;

  @Override
  public void getNotifications(UUID userId, UUID cursorId, int size) {
    notificationRepository.findByUserIdWithCursor(userId, cursorId, size);
  }
}