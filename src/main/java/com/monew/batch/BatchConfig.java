package com.monew.batch;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

  @Bean
  public Tasklet sampleTasklet(MeterRegistry meterRegistry, com.monew.service.ArticleService articleService) {
    return (contribution, chunkContext) -> {
      // 실제 데이터 수집 로직을 ArticleService로 위임
      try {
        articleService.collect();
        meterRegistry.counter("batch.news.executions").increment();
      } catch (Exception e) {
        meterRegistry.counter("batch.news.failures").increment();
        throw e;
      }
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
    return new JobBuilder("sampleJob", jobRepository).start(sampleStep).build();
  }

  @Bean
  public Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet sampleTasklet) {
    return new StepBuilder("sampleStep", jobRepository).tasklet(sampleTasklet, transactionManager).build();
  }
}




