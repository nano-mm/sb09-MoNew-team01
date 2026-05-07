package com.monew.adapter.out.storage.log.impl;

import com.monew.application.port.out.storage.log.LogStorage;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "monew.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class S3LogStorage implements LogStorage {

  private final S3Client s3Client;

  @Value("${monew.storage.s3.bucket}")
  private String s3Bucket;

  private static final String BASE_DIR = "logs/";

  @Override
  public void backup(File logFile, String fileName) {
    String key = BASE_DIR + fileName;

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key(key)
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromFile(logFile));
    log.info("[로그 백업] S3 버킷에 로그 적재 완료: s3://{}/{}", s3Bucket, key);
  }
}
