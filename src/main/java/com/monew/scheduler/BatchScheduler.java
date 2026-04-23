package com.monew.scheduler;

import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
@RequiredArgsConstructor
public class BatchScheduler implements SchedulingConfigurer {

  private final List<BatchTask> batchTaskList;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    for (BatchTask task : batchTaskList) {
      taskRegistrar.addTriggerTask(
          task::execute,
          // 한국 시간으로 고정
          new CronTrigger(task.getCron(), ZoneId.of("Asia/Seoul"))
      );
    }
  }
}
