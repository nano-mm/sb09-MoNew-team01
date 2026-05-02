package com.monew.storage.backup;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.io.Resource;

public interface ArticleBackupStorage {

  void saveBackup(String fileName, String jsonData) throws IOException;

  List<Resource> loadBackupResources(LocalDateTime from, LocalDateTime to) throws IOException;

}
