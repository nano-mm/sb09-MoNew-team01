package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.repository.UserRepository;
import com.monew.scheduler.task.HardDeleteTask;
import com.monew.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.data.mongodb.database=testdb")
@Transactional
class HardDeleteTaskTest {

  @Autowired
  private HardDeleteTask hardDeleteTask;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("논리 삭제 후 24시간이 지난 사용자만 물리 삭제되어야 한다")
  void hardDeleteTaskTest() {
    // given
    Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
    Instant tenHoursAgo = Instant.now().minus(10, ChronoUnit.HOURS);

    User user1 = insertUser("old@test.com", twentyFiveHoursAgo);
    User user2 = insertUser("recent@test.com", tenHoursAgo);
    User user3 = insertUser("active@test.com", null);

    // when
    hardDeleteTask.execute();

    // then
    assertThat(userRepository.existsById(user1.getId())).isFalse();
    assertThat(userRepository.existsById(user2.getId())).isTrue();
    assertThat(userRepository.existsById(user3.getId())).isTrue();
  }

  private User insertUser(String email, Instant deletedAt) {
    User user = User.builder()
        .email(email)
        .nickname("tester")
        .password("password123!")
        .deletedAt(deletedAt)
        .build();

    return userRepository.save(user);
  }
}