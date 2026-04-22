package com.monew.service;

import java.io.IOException;

public interface ArticleBackupService {
  public void export() throws IOException;

  public void importBackup() throws IOException;
}
