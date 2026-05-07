package com.monew.adapter.out.storage.backup.impl;

import com.monew.application.port.out.storage.backup.ArticleBackupStorage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "monew.storage.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalArticleBackupStorage implements ArticleBackupStorage {

  private final ResourcePatternResolver resourcePatternResolver;

  private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Value("${monew.storage.local.root-path}")
  private String localRootPath;

  private String getDirPath() {
    return localRootPath.endsWith("/") ? localRootPath + "backups/" : localRootPath + "/backups/";
  }

  @Override
  public void saveBackup(String fileName, String jsonData) throws IOException {
    String fullPath = "file:" + getDirPath() + fileName;
    Resource resource = resourcePatternResolver.getResource(fullPath);

    File parentDir = resource.getFile().getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }

    if (resource instanceof WritableResource writableResource) {
      try (OutputStream os = writableResource.getOutputStream()) {
        os.write(jsonData.getBytes(StandardCharsets.UTF_8));
      }
    }
    log.info("[뉴스 기사 백업] 로컬 스토리지에 파일 저장 완료: {}", fullPath);
  }

  @Override
  public List<Resource> loadBackupResources(LocalDateTime from, LocalDateTime to) throws IOException {
    String pattern = "file:" + getDirPath() + "*.json";
    Resource[] resources = resourcePatternResolver.getResources(pattern);

    LocalDate fromDate = from.toLocalDate();
    LocalDate toDate = to.toLocalDate();

    return Arrays.stream(resources)
        .filter(resource -> isWithinDateRange(resource.getFilename(), fromDate, toDate))
        .collect(Collectors.toList());
  }


  private boolean isWithinDateRange(String fileName, LocalDate fromDate, LocalDate toDate) {
    if (fileName == null) return false;

    try {
      String dateString = fileName.replace("backup_", "").replace(".json", "");
      LocalDate fileDate = LocalDate.parse(dateString, FILE_DATE_FORMATTER);

      return !fileDate.isBefore(fromDate) && !fileDate.isAfter(toDate);

    } catch (Exception e) {
      log.warn("[뉴스 기사 백업] 로컬 파일명 날짜 파싱 실패. 파일명: {}", fileName);
      return false;
    }
  }
}
