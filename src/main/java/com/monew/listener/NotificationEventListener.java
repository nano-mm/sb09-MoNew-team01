package com.monew.listener;

import com.monew.event.NotificationCreatedEvent;
import com.monew.domain.model.Notification;
import com.monew.domain.model.User;
import com.monew.application.port.out.persistence.NotificationRepository;
import com.monew.application.port.out.persistence.UserRepository;
import com.monew.domain.model.enums.ResourceType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleNotificationCreated(NotificationCreatedEvent event) {
    if (event == null || event.userId() == null || event.content() == null || event.resourceType() == null || event.resourceId() == null) {
        throw new IllegalArgumentException("Invalid event: all fields must be non-null");
    }

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
