package com.monew.scheduler;

public interface BatchTask {
  void execute();
  String getCron();
}
