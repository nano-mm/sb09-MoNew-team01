package com.monew.unit.storage.backup.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.monew.adapter.out.storage.backup.impl.S3ArticleBackupStorage;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupStorageTest {

  @Mock
  S3Client s3Client;

  @Mock
  ResourceLoader resourceLoader;

  @InjectMocks
  S3ArticleBackupStorage storage;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(storage, "s3Bucket", "test-bucket");
  }

  @Test
  void saveBackup_정상동작() {
    // when
    storage.saveBackup("test.json", "{}");

    // then
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void loadBackupResources_정상동작() {
    // given
    S3Object s3Object = S3Object.builder()
        .key("backups/backup_2015-12-17.json")
        .build();

    ListObjectsV2Response response = ListObjectsV2Response.builder()
        .contents(s3Object)
        .build();

    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);
    when(resourceLoader.getResource(anyString())).thenReturn(mock(Resource.class));

    LocalDateTime from = LocalDateTime.of(2000, 1, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2100, 12, 31, 23, 59);

    // when
    List<Resource> result = storage.loadBackupResources(from, to);

    // then
    assertThat(result).hasSize(1);
  }
}