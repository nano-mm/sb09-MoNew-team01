package com.monew.listener;

import com.monew.event.NotificationCreatedEvent;
import com.monew.entity.Notification;
import com.monew.entity.User;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import com.monew.entity.enums.ResourceType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @EventListener
  @Transactional
  public void handleNotificationCreated(NotificationCreatedEvent event) {
    UUID userId = event.userId();
    String content = event.content();
    ResourceType type = event.resourceType();
    UUID resourceId = event.resourceId();

    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      log.warn("[알림 이벤트] 사용자 없음 userId={}", userId);
      return;
    }

    Notification notification = Notification.of(user, content, type, resourceId);
    notificationRepository.save(notification);
    log.debug("[알림 이벤트] 알림 생성 이벤트 처리 완료 userId={} resourceId={}", userId, resourceId);
  }
}

