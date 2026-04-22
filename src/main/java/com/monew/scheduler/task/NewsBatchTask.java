package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.service.ArticleService;
import com.monew.batch.BatchJobLauncher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchTask implements BatchTask {

  private final ArticleService articleService;
  private final BatchJobLauncher batchJobLauncher;

  @Value("${monew.batch.news.cron}")
  private String cron;

  @Override
  public void execute() {
    // Spring Batch job으로 데이터 수집을 위임
    batchJobLauncher.launchSampleJob();
  }

  @Override
  public String getCron() {
    return this.cron;
  }
}
