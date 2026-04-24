package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.service.ArticleBackupService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBackupBatchTask implements BatchTask {
  private final ArticleBackupService articleBackupService;

  @Value("${monew.batch.news-backup.cron}")
  private String cron;

  @Override
  public void execute() {
    try {
      articleBackupService.export();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getCron() {
    return this.cron;
  }
}
