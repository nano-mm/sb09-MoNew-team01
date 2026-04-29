package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.storage.log.LogStorage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Component
@ConditionalOnProperty(name = "monew.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class LogBackupTask implements BatchTask {

  private final LogStorage logStorage;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Value("${monew.batch.log-backup.cron}")
  private String cron;

  @Override
  public String getCron() {
    return this.cron;
  }

  @Override
  public String getJobName() {
    return "logBackupJob";
  }

  @Override
  public Job getJob() {
    return new JobBuilder(this.getJobName(), jobRepository)
        .start(logBackupStep())
        .build();
  }

  private Step logBackupStep() {
    return new StepBuilder("logBackupStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {

          LocalDate yesterday = LocalDate.now().minusDays(1);
          process(yesterday);

          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  private void process(LocalDate targetDate) {
    String dateString = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    int uploadCount = 0;

    String localFolderPath = "./logs/" + dateString + "/";

    log.info("[로그 백업] {} 날짜의 S3 적재 시작", dateString);

    for (int hour = 0; hour < 24; hour++) {
      String hourStr = String.format("%02d", hour);
      String fileName = "monew." + dateString + "_" + hourStr + ".log";

      File logFile = new File(localFolderPath + fileName);

      if (logFile.exists() && logFile.isFile()) {
        try {
          String s3Key = dateString + "/" + fileName;

          logStorage.backup(logFile, s3Key);
          uploadCount++;
        } catch (Exception e) {
          log.error("[로그 백업] {} 적재 실패", fileName, e);
        }
      }
    }
    log.info("[로그 백업] {} 완료. 총 {}개 파일 적재됨", dateString, uploadCount);
  }

  // 프로그램 실행 시 어제자 로그 백업
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info("[로그 백업] 어제자 로그 S3 적재 시도");
    LocalDate yesterday = LocalDate.now().minusDays(1);
    process(yesterday);
  }
}
