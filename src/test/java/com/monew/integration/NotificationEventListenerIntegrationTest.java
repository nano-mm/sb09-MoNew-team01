package com.monew.integration.listener;

import com.monew.event.NotificationCreatedEvent;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationEventListenerIntegrationTest {

  @Autowired
  ApplicationEventPublisher publisher;

  @Autowired
  NotificationRepository notificationRepository;

  @Autowired
  UserRepository userRepository;

  @Test
  void whenEventPublished_notificationIsPersisted() {
    // prepare user
    User user = User.of("int@test.monew", "integ", "pwd");
    User saved = userRepository.save(user);

    final UUID savedUserId = saved.getId();
    final UUID resourceId = UUID.randomUUID();

    publisher.publishEvent(new NotificationCreatedEvent(savedUserId, "hello integ", ResourceType.INTEREST, resourceId));

    List<?> all = notificationRepository.findAll();
    boolean found = false;
    for (Object o : all) {
      try {
        com.monew.entity.Notification notif = (com.monew.entity.Notification) o;
        if (notif.getUser().getId().equals(savedUserId) && notif.getResourceId().equals(resourceId)) {
          found = true;
          break;
        }
      } catch (Exception e) {
        // ignore
      }
    }

    assertThat(found).isTrue();
  }
}


