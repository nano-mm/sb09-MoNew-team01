package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.storage.log.LogStorage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "monew.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class LogBackupTask implements BatchTask {

  private final LogStorage logStorage;

  @Value("${monew.batch.log-backup.cron}")
  private String cron;

  @Override
  public void execute() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    process(yesterday);
  }

//  // 테스트 목적으로 1분 간격 백업하도록 만든 것
//  // <fileNamePattern>${LOG_PATH}/%d{yyyy-MM-dd, aux}/monew.%d{yyyy-MM-dd_HHmm}.log</fileNamePattern> <- logback-spring.xml 수정해야함
//  @Override
//  public void execute() {
//    log.info("[테스트 로그 백업] Logback 롤링 강제 트리거용 로그입니다.");
//
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//    }
//    LocalDateTime lastMinute = LocalDateTime.now().minusMinutes(1);
//    processTest(lastMinute);
//  }

//  private void processTest(LocalDateTime targetTime) {
//    String dateFolder = targetTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//    String timeString = targetTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
//    String fileName = "monew." + timeString + ".log";
//
//    String folderPath = "./logs/" + dateFolder + "/";
//    File logFile = new File(folderPath + fileName);
//
//    log.info("[테스트 로그 백업] {} 파일 확인 중...", folderPath + fileName);
//
//    if (logFile.exists() && logFile.isFile()) {
//      try {
//        String s3Key = dateFolder + "/" + fileName;
//        logStorage.backup(logFile, s3Key);
//
//        log.info("[테스트 로그 백업] S3 경로 [{}] 적재 완료!", s3Key);
//      } catch (Exception e) {
//        log.error("[테스트 로그 백업] {} 적재 실패", fileName, e);
//      }
//    } else {
//      log.warn("[테스트 로그 백업] 파일을 찾을 수 없습니다. 경로 오류 의심: {}", logFile.getAbsolutePath());
//    }
//  }

  @Override
  public String getCron() {
    return this.cron;
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
