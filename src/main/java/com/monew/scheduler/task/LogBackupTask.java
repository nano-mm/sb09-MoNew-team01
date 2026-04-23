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
//  // <fileNamePattern>${LOG_PATH}/monew.%d{yyyy-MM-dd_HHmm}.log</fileNamePattern> <- logback-spring.xml 수정해야함
//  @Override
//  public void execute() {
//    LocalDateTime lastMinute = LocalDateTime.now().minusMinutes(1);
//    processTest(lastMinute);
//  }

  private void processTest(LocalDateTime targetTime) {
    String timeString = targetTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
    String fileName = "monew." + timeString + ".log";
    File logFile = new File("./logs/" + fileName);

    log.info("[테스트 로그 백업] {} 파일 확인 중...", fileName);

    if (logFile.exists() && logFile.isFile()) {
      try {
        logStorage.backup(logFile, fileName);
        log.info("[테스트 로그 백업] {} 적재 완료!", fileName);
      } catch (Exception e) {
        log.error("[테스트 로그 백업] 적재 실패", e);
      }
    } else {
      log.warn("[테스트 로그 백업] 파일을 찾을 수 없습니다. (롤링 대기 중이거나 경로 오류)");
    }
  }

  @Override
  public String getCron() {
    return this.cron;
  }

  private void process(LocalDate targetDate) {
    String dateString = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    int uploadCount = 0;
    log.info("[로그 백업] {} 날짜의 시간대별 로그 수집 시작", dateString);

    for (int hour = 0; hour < 24; hour++) {
      String hourStr = String.format("%02d", hour);
      String fileName = "monew." + dateString + "_" + hourStr + ".log";
      File logFile = new File("./logs/" + fileName);

      if (logFile.exists() && logFile.isFile()) {
        try {
          logStorage.backup(logFile, fileName);
          uploadCount++;
        } catch (Exception e) {
          log.error("[로그 백업] {}시 로그 적재 실패", hourStr, e);
        }
      }
    }
    log.info("[로그 백업] {} 날짜 완료. 총 {}개의 파일 적재 완료", dateString, uploadCount);
  }
}
