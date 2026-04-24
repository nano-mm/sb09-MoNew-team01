package com.monew.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchScheduler implements SchedulingConfigurer {

  private final List<BatchTask> batchTaskList;

  private final JobLauncher jobLauncher;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final MeterRegistry meterRegistry;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    for (BatchTask task : batchTaskList) {
      taskRegistrar.addTriggerTask(
          () -> executeAsSpringBatchJob(task),
          new CronTrigger(task.getCron(), ZoneId.of("Asia/Seoul"))
      );
    }
  }

  private void executeAsSpringBatchJob(BatchTask task) {
    String taskName = task.getClass().getSimpleName();
    String jobName = taskName + "Job";

    Timer timer = Timer.builder("monew.batch.execution.time")
        .description("배치 작업 소요 시간")
        .tag("taskName", taskName)
        .register(meterRegistry);

    try {
      // 지금은 step 하나씩만 만들어서 각 BatchTask 하나씩 실행
      Step step = new StepBuilder(taskName + "Step", jobRepository)
          .tasklet((contribution, chunkContext) -> {

            // 시간 측정
            timer.record(() -> task.execute());

            meterRegistry.counter("monew.batch.execution.status", "taskName", taskName, "status", "SUCCESS")
                .increment();

            return RepeatStatus.FINISHED;

          }, transactionManager)
          .build();

      Job job = new JobBuilder(jobName, jobRepository)
          .start(step)
          .build();

      JobParameters params = new JobParametersBuilder()
          .addLong("runTime", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(job, params);

    } catch (Exception e) {
      meterRegistry.counter("monew.batch.execution.status", "taskName", taskName, "status", "FAIL")
          .increment();
      log.error("[Spring Batch] 런칭 실패 JobName: {}", jobName, e);
    }
  }
}