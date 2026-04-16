package com.monew.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@RequiredArgsConstructor
public class BatchScheduler implements SchedulingConfigurer {

  private final List<BatchTask> batchTaskList;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    for (BatchTask task : batchTaskList) {
      taskRegistrar.addCronTask(
          task::execute,
          task.getCron()
      );
    }
  }
}
