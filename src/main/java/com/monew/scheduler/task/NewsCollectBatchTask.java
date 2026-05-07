package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.application.service.ArticleService;
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
public class NewsCollectBatchTask implements BatchTask {

  private final ArticleService articleService;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Value("${monew.batch.news.cron}")
  private String cron;

  @Override
  public String getCron() {
    return this.cron;
  }

  @Override
  public String getJobName() {
    return "newsCollectJob";
  }

  @Override
  public Job getJob() {
    return new JobBuilder(this.getJobName(), jobRepository)
        .start(collectNewsStep())
        .build();
  }

  private Step collectNewsStep() {
    return new StepBuilder("collectNewsStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          articleService.collect();
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}
