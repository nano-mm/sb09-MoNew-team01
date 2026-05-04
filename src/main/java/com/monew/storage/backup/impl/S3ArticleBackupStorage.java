package com.monew.storage.backup.impl;

import com.monew.storage.backup.ArticleBackupStorage;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "monew.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class S3ArticleBackupStorage implements ArticleBackupStorage {

  private final S3Client s3Client;
  private final ResourceLoader resourceLoader;

  @Value("${monew.storage.s3.bucket}")
  private String s3Bucket;

  private static final String BASE_DIR = "backups/";
  private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public void saveBackup(String fileName, String jsonData) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key(BASE_DIR + fileName)
        .contentType("application/json")
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonData));

    log.info("[뉴스 기사 백업]S3 버킷에 파일 저장 완료: s3://{}/{}", s3Bucket, BASE_DIR + fileName);
  }

  @Override
  public List<Resource> loadBackupResources(LocalDateTime from, LocalDateTime to) {
    List<Resource> backupResources = new ArrayList<>();

    ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
        .bucket(s3Bucket)
        .prefix(BASE_DIR)
        .build();

    LocalDate fromDate = from.toLocalDate();
    LocalDate toDate = to.toLocalDate();

    s3Client.listObjectsV2(listRequest).contents().stream()
        .filter(s3Object -> s3Object.key().endsWith(".json"))
        .filter(s3Object -> isWithinDateRange(s3Object.key(), fromDate, toDate))
        .forEach(s3Object -> {
          String s3Uri = "s3://" + s3Bucket + "/" + s3Object.key();
          backupResources.add(resourceLoader.getResource(s3Uri));
        });

    return backupResources;
  }


  private boolean isWithinDateRange(String key, LocalDate fromDate, LocalDate toDate) {
    if (key == null) return false;

    try {
      String fileName = key.substring(key.lastIndexOf("/") + 1);

      String dateString = fileName.replace("backup_", "").replace(".json", "");

      LocalDate fileDate = LocalDate.parse(dateString, FILE_DATE_FORMATTER);

      return !fileDate.isBefore(fromDate) && !fileDate.isAfter(toDate);

    } catch (Exception e) {
      log.warn("[뉴스 기사 백업] S3 파일명 날짜 파싱 실패. key: {}", key);
      return false;
    }
  }
}
