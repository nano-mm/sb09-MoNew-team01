package com.monew.application.port.out.storage.log;

import java.io.File;

public interface LogStorage {
  void backup(File logFile, String fileName);
}
