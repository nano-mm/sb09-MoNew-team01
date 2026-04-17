package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HardDeleteTask implements BatchTask {

  private final JdbcTemplate jdbcTemplate;

  @Value("${monew.batch.user-hard-delete.cron:0 0 3 * * *}")
  private String cron;

  @Override
  public void execute() {
    log.info("논리 삭제된 데이터의 물리 삭제 작업을 시작합니다.");
    Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
    String sql = "DELETE FROM users WHERE is_deleted = true AND deleted_at < ?";

    try {
      int rows = jdbcTemplate.update(sql, oneDayAgo);
      log.info("물리 삭제 완료. 삭제된 행 수: {}", rows);
    } catch (Exception e) {
      log.error("물리 삭제 중 오류 발생: {}", e.getMessage());
    }
  }

  @Override
  public String getCron() {
    return this.cron;
  }
}