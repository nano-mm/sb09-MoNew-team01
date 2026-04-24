package com.monew.service;

import com.monew.dto.response.ArticleRestoreResultDto;
import java.io.IOException;
import java.time.LocalDateTime;

public interface ArticleBackupService {
  void export() throws IOException;

  ArticleRestoreResultDto importBackup(LocalDateTime from, LocalDateTime to) throws IOException;
}
