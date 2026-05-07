package com.monew.scheduler.task;

import com.monew.adapter.out.persistence.UserRepository;
import com.monew.scheduler.BatchTask;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class HardDeleteTask implements BatchTask {

  private final UserRepository userRepository;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Value("${monew.batch.user-hard-delete.cron:0 0 3 * * *}")
  private String cron;


  @Override
  public String getCron() {
    return this.cron;
  }

  @Override
  public String getJobName() {
    return "hardDeleteJob";
  }

  @Override
  public Job getJob() {
    return new JobBuilder(this.getJobName(), jobRepository)
        .start(userHardDeleteStep())
        .build();
  }

  private Step userHardDeleteStep() {
    return new StepBuilder("userHardDeleteStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {

          log.info("논리 삭제된 데이터의 물리 삭제 작업을 시작합니다.");
          Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

          try {
            int rows = userRepository.deleteSoftDeletedUsersOlderThan(oneDayAgo);
            log.info("물리 삭제 완료. 삭제된 행 수: {}", rows);
          } catch (Exception e) {
            log.error("물리 삭제 중 오류 발생: {}", e.getMessage());
          }

          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}