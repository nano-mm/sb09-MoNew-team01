package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.config.JpaAuditConfig;
import com.monew.config.TestQueryDslConfig;
import com.monew.domain.model.Notification;
import com.monew.domain.model.User;
import com.monew.domain.model.enums.ResourceType;
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
  private com.monew.application.port.out.persistence.NotificationRepository notificationRepository;

  @Autowired
  private com.monew.application.port.out.persistence.UserRepository userRepository;

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

  @Test
  void findByIdAndUserIdAndConfirmedFalse_returnsNotification() {
    User user = saveUser("user1@monew.com", "user1");
    Notification notification = saveNotification(user, "content", Instant.now(), false);

    Optional<Notification> result = notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notification.getId(), user.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(notification.getId());
  }

  @Test
  void findAllByUserIdAndConfirmedFalse_returnsUnconfirmedNotifications() {
    User user = saveUser("user1@monew.com", "user1");
    saveNotification(user, "content1", Instant.now(), false);
    saveNotification(user, "content2", Instant.now(), false);

    List<Notification> notifications = notificationRepository.findAllByUser_IdAndConfirmedFalse(user.getId());

    assertThat(notifications).hasSize(2);
    assertThat(notifications).allSatisfy(notification -> assertThat(notification.getConfirmed()).isFalse());
  }

  @Test
  void countByUserIdAndConfirmedFalse_returnsCount() {
    User user = saveUser("user1@monew.com", "user1");
    saveNotification(user, "content1", Instant.now(), false);
    saveNotification(user, "content2", Instant.now(), false);

    long count = notificationRepository.countByUser_IdAndConfirmedFalse(user.getId());

    assertThat(count).isEqualTo(2);
  }

  @Test
  void deleteByConfirmedIsTrueAndCreatedAtBefore_deletesOldConfirmedNotifications() {
    User user = saveUser("user1@monew.com", "user1");
    saveNotification(user, "oldConfirmed", Instant.now().minusSeconds(3600), true);
    saveNotification(user, "recentConfirmed", Instant.now(), true);

    long deletedCount = notificationRepository.deleteByConfirmedIsTrueAndCreatedAtBefore(Instant.now().minusSeconds(1800));

    assertThat(deletedCount).isEqualTo(1);
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

  @Test
  void findByUserIdWithCursor_whenCursorExists_returnsOlderNotifications() {
    User user = saveUser("user1@monew.com", "user1");

    Notification newest = saveNotification(user, "new", Instant.parse("2026-04-19T12:00:03Z"), false);
    Notification middle = saveNotification(user, "middle", Instant.parse("2026-04-19T12:00:02Z"), false);
    Notification oldest = saveNotification(user, "old", Instant.parse("2026-04-19T12:00:01Z"), false);

    entityManager.clear();

    List<Notification> result =
        notificationRepository.findByUserIdWithCursor(user.getId(), Instant.parse("2026-04-19T12:00:03Z"), 10);

    assertThat(result)
        .extracting(Notification::getId)
        .containsExactly(middle.getId(), oldest.getId());
  }

  @Test
  void findByUserIdWithCursor_whenSizeIsZero_returnsAtLeastOne() {
    User user = saveUser("user1@monew.com", "user1");

    Notification n1 = saveNotification(user, "n1", Instant.now(), false);

    entityManager.clear();

    List<Notification> result =
        notificationRepository.findByUserIdWithCursor(user.getId(), null, 0);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(n1.getId());
  }

  @Test
  void findByUserIdAndConfirmedFalse_returnsOnlyUnconfirmed() {
    User user = saveUser("user1@monew.com", "user1");

    saveNotification(user, "confirmed", Instant.now(), true);
    Notification unconfirmed = saveNotification(user, "unconfirmed", Instant.now(), false);

    entityManager.clear();

    List<Notification> result =
        notificationRepository.findByUserIdAndConfirmedFalse(user.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(unconfirmed.getId());
  }

}
