package com.monew.storage.backup.impl;

import com.monew.storage.backup.BackupStorage;
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
public class LocalBackupStorage implements BackupStorage {

  private final ResourcePatternResolver resourcePatternResolver;

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
  public List<Resource> loadBackupResources() throws IOException {
    String pattern = "file:" + getDirPath() + "*.json";
    Resource[] resources = resourcePatternResolver.getResources(pattern);
    return Arrays.asList(resources);
  }
}
