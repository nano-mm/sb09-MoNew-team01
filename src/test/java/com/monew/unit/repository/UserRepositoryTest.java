package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.entity.User;
import com.monew.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import com.monew.config.TestQueryDslConfig;

@DataJpaTest
@Import(TestQueryDslConfig.class)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EntityManager em;

  @Test
  @DisplayName("논리 삭제된 유저를 포함하여 이메일 중복 여부를 확인한다")
  void existsInAllUsers() {
    // given
    String email = "deleted@test.com";
    User user = User.of(email, "tester", "pw");
    user.markAsDeleted(Instant.now());
    userRepository.save(user);
    em.flush();
    em.clear();

    // when
    boolean exists = userRepository.existsByEmail(email);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("24시간이 지난 논리 삭제 유저를 물리 삭제한다")
  void deleteSoftDeletedUsersOlderThan() {
    // given
    User oldDeleted = User.of("old@test.com", "old", "pw");
    oldDeleted.markAsDeleted(Instant.now());
    userRepository.save(oldDeleted);
    
    User recentDeleted = User.of("recent@test.com", "recent", "pw");
    recentDeleted.markAsDeleted(Instant.now());
    userRepository.save(recentDeleted);

    em.flush();

    // 강제로 deleted_at 시간 변경
    jdbcTemplate.update("UPDATE users SET deleted_at = ? WHERE email = ?", 
        Instant.now().minus(25, ChronoUnit.HOURS), "old@test.com");
    jdbcTemplate.update("UPDATE users SET deleted_at = ? WHERE email = ?", 
        Instant.now().minus(1, ChronoUnit.HOURS), "recent@test.com");

    em.clear();

    // when
    int deletedCount = userRepository.deleteSoftDeletedUsersOlderThan(Instant.now().minus(24, ChronoUnit.HOURS));

    // then
    assertThat(deletedCount).isEqualTo(1);
    assertThat(userRepository.existsByEmail("old@test.com")).isFalse();
    assertThat(userRepository.existsByEmail("recent@test.com")).isTrue();
  }

  @Test
  @DisplayName("물리적 ID 존재 여부를 확인하고 삭제한다")
  void physicalIdOperations() {
    // given
    User user = User.of("physical@test.com", "tester", "pw");
    userRepository.save(user);
    em.flush();
    UUID userId = user.getId();
    em.clear();

    // when & then (존재 확인)
    assertThat(userRepository.existsById(userId)).isTrue();

    // when (물리 삭제)
    userRepository.deleteById(userId);

    // then (삭제 확인)
    assertThat(userRepository.existsById(userId)).isFalse();
  }

  @Test
  @DisplayName("이메일로 유저를 조회한다 (Soft Delete 필터링 확인)")
  void findByEmailAndDeletedAtIsNull() {
    // given
    String email = "find@test.com";
    User user = User.of(email, "tester", "pw");
    userRepository.save(user);
    em.flush();
    em.clear();

    // when
    Optional<User> found = userRepository.findByEmailAndDeletedAtIsNull(email);

    // then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo(email);

    // when
    User target = userRepository.findById(found.get().getId()).orElseThrow();
    target.markAsDeleted(Instant.now());
    em.flush();
    em.clear();
    
    Optional<User> afterDelete = userRepository.findByEmailAndDeletedAtIsNull(email);

    // then
    assertThat(afterDelete).isEmpty();
  }
}
