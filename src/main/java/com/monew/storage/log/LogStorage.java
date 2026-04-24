package com.monew.storage.log;

import java.io.File;

public interface LogStorage {
  void backup(File logFile, String fileName);
}
