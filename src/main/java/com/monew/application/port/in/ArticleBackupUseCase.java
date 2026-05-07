package com.monew.application.port.in;

import com.monew.dto.response.ArticleRestoreResultDto;
import java.io.IOException;
import java.time.LocalDateTime;

public interface ArticleBackupUseCase {
  void export(LocalDateTime from, LocalDateTime to) throws IOException;
  ArticleRestoreResultDto importBackup(LocalDateTime from, LocalDateTime to) throws IOException;
}
