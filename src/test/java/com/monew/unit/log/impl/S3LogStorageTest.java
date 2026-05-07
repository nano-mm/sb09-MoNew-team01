package com.monew.unit.log.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.monew.adapter.out.storage.log.impl.S3LogStorage;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3LogStorageTest {

  @Mock
  S3Client s3Client;

  @InjectMocks
  S3LogStorage storage;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(storage, "s3Bucket", "test-bucket");
  }

  @Test
  void backup_정상동작() throws Exception {
    // given
    File file = File.createTempFile("test", ".log");

    // when
    storage.backup(file, "test.log");

    // then
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // cleanup
    file.delete();
  }
}