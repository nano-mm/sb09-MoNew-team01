package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchTask implements BatchTask {

  private final ArticleService articleService;

  @Value("${monew.batch.news.cron}")
  private String cron;

  @Override
  public void execute() {
    articleService.collect();
  }

  @Override
  public String getCron() {
    return this.cron;
  }
}
