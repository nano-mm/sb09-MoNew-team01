package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.application.service.ArticleBackupService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class NewsBackupBatchTask implements BatchTask {
  private final ArticleBackupService articleBackupService;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Value("${monew.batch.news-backup.cron}")
  private String cron;

  @Override
  public String getCron() {
    return this.cron;
  }

  @Override
  public String getJobName() {
    return "newsBackupJob";
  }

  @Override
  public Job getJob() {
    return new JobBuilder(this.getJobName(), jobRepository)
        .start(articleBackupStep())
        .build();
  }


  private Step articleBackupStep() {
    return new StepBuilder("articleBackupStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          try {
            // 전날 기사 백업
            LocalDateTime from = LocalDate.now().minusDays(1).atStartOfDay();
            LocalDateTime to = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);
            articleBackupService.export(from, to);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}
