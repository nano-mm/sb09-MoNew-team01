package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.config.JpaAuditConfig;
import com.monew.config.TestQueryDslConfig;
import com.monew.entity.Notification;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({JpaAuditConfig.class, TestQueryDslConfig.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

  @Autowired
  private com.monew.repository.NotificationRepository notificationRepository;

  @Autowired
  private com.monew.repository.UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void findByUserIdWithCursor_whenCursorIsNull_returnsUnconfirmedNotificationsInNewestOrder() {
    User user = saveUser("user1@monew.com", "user1");
    User otherUser = saveUser("user2@monew.com", "user2");

    Notification latest = saveNotification(user, "latest", Instant.parse("2026-04-19T12:00:03Z"), false);
    saveNotification(user, "confirmed", Instant.parse("2026-04-19T12:00:02Z"), true);
    Notification older = saveNotification(user, "older", Instant.parse("2026-04-19T12:00:01Z"), false);
    saveNotification(otherUser, "other", Instant.parse("2026-04-19T12:00:04Z"), false);

    entityManager.clear();

    List<Notification> notifications = notificationRepository.findByUserIdWithCursor(user.getId(), null, 10);

    assertThat(notifications)
        .extracting(Notification::getId)
        .containsExactly(latest.getId(), older.getId());
    assertThat(notifications).allSatisfy(notification -> assertThat(notification.getConfirmed()).isFalse());
  }

  private User saveUser(String email, String nickname) {
    User user = User.of(email, nickname, "password");
    return userRepository.saveAndFlush(user);
  }

  private Notification saveNotification(User user, String content, Instant createdAt, boolean confirmed) {
    Notification notification = Notification.of(user, content, ResourceType.INTEREST, UUID.randomUUID());
    if (confirmed) {
      notification.confirm();
    }
    Notification saved = notificationRepository.saveAndFlush(notification);

    entityManager.getEntityManager().createNativeQuery(
            "update notifications set created_at = ?1, updated_at = ?2 where id = ?3")
        .setParameter(1, Timestamp.from(createdAt))
        .setParameter(2, Timestamp.from(createdAt))
        .setParameter(3, saved.getId())
        .executeUpdate();

    ReflectionTestUtils.setField(saved, "createdAt", createdAt);
    ReflectionTestUtils.setField(saved, "updatedAt", createdAt);
    return saved;
  }
}

