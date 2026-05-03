package com.monew.unit.storage.backup.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.monew.storage.backup.impl.LocalArticleBackupStorage;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocalArticleBackupStorageTest {

  @Mock
  ResourcePatternResolver resolver;

  @Mock
  WritableResource resource;

  @InjectMocks
  LocalArticleBackupStorage storage;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(storage, "localRootPath", "/tmp");
  }

  @Test
  void saveBackup_정상동작() throws Exception {
    // given
    String fileName = "test.json";
    String data = "{}";

    File mockFile = mock(File.class);
    File parent = mock(File.class);

    when(resolver.getResource(anyString())).thenReturn(resource);
    when(resource.getFile()).thenReturn(mockFile);
    when(mockFile.getParentFile()).thenReturn(parent);
    when(parent.exists()).thenReturn(true);

    OutputStream os = mock(OutputStream.class);
    when(resource.getOutputStream()).thenReturn(os);

    // when
    storage.saveBackup(fileName, data);

    // then
    verify(os).write(any(byte[].class));
  }

  @Test
  void loadBackupResources_정상동작() throws Exception {
    // given
    Resource[] resources = new Resource[]{resource};
    when(resolver.getResources(anyString())).thenReturn(resources);

    // when
    List<Resource> result = storage.loadBackupResources();

    // then
    assertThat(result).hasSize(1);
  }
}