package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.application.port.out.persistence.UserRepository;
import com.monew.scheduler.task.HardDeleteTask;
import com.monew.domain.model.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBatchTest
@SpringBootTest(properties = "spring.data.mongodb.database=testdb")
@TestPropertySource(properties = "spring.batch.jdbc.initialize-schema=always")
class HardDeleteTaskTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private HardDeleteTask hardDeleteTask;

  @Autowired
  private UserRepository userRepository;

  private User user1;
  private User user2;
  private User user3;

  @AfterEach
  void tearDown() {
    if (user1 != null && user1.getId() != null) userRepository.deleteById(user1.getId());
    if (user2 != null && user2.getId() != null) userRepository.deleteById(user2.getId());
    if (user3 != null && user3.getId() != null) userRepository.deleteById(user3.getId());
  }

  @Test
  @DisplayName("논리 삭제 후 24시간이 지난 사용자만 물리 삭제되어야 한다")
  void hardDeleteTaskTest() throws Exception {
    // given
    Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
    Instant tenHoursAgo = Instant.now().minus(10, ChronoUnit.HOURS);

    user1 = insertUser("old@test.com", twentyFiveHoursAgo);
    user2 = insertUser("recent@test.com", tenHoursAgo);
    user3 = insertUser("active@test.com", null);

    jobLauncherTestUtils.setJob(hardDeleteTask.getJob());

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

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