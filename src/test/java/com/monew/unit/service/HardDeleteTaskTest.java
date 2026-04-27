package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.scheduler.task.HardDeleteTask;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBatchTest
@SpringBootTest(properties = "spring.data.mongodb.database=testdb")
class HardDeleteTaskTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private HardDeleteTask hardDeleteTask;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private UUID user1_OldDeleted;
  private UUID user2_RecentDeleted;
  private UUID user3_Active;

  @AfterEach
  void tearDown() {
    if (user1_OldDeleted != null) jdbcTemplate.update("DELETE FROM users WHERE id = ?", user1_OldDeleted);
    if (user2_RecentDeleted != null) jdbcTemplate.update("DELETE FROM users WHERE id = ?", user2_RecentDeleted);
    if (user3_Active != null) jdbcTemplate.update("DELETE FROM users WHERE id = ?", user3_Active);
  }

  @Test
  @DisplayName("논리 삭제 후 24시간이 지난 사용자만 물리 삭제되어야 한다")
  void hardDeleteTaskTest() throws Exception {
    // given
    user1_OldDeleted = UUID.randomUUID();
    user2_RecentDeleted = UUID.randomUUID();
    user3_Active = UUID.randomUUID();
    
    Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
    Instant tenHoursAgo = Instant.now().minus(10, ChronoUnit.HOURS);

    insertUser(user1_OldDeleted, "old@test.com", true, twentyFiveHoursAgo);
    insertUser(user2_RecentDeleted, "recent@test.com", true, tenHoursAgo);
    insertUser(user3_Active, "active@test.com", false, null);

    jobLauncherTestUtils.setJob(hardDeleteTask.getJob());

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(countUserById(user1_OldDeleted)).isEqualTo(0);
    assertThat(countUserById(user2_RecentDeleted)).isEqualTo(1);
    assertThat(countUserById(user3_Active)).isEqualTo(1);
  }

  private void insertUser(UUID id, String email, boolean isDeleted, Instant deletedAt) {
    String sql = "INSERT INTO users (id, email, nickname, password, is_deleted, deleted_at) VALUES (?, ?, ?, ?, ?, ?)";
    jdbcTemplate.update(sql, id, email, "tester", "password123!", isDeleted, deletedAt);
  }

  private int countUserById(UUID id) {
    String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, id);
  }
}