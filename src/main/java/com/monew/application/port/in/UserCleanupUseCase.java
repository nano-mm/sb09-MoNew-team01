package com.monew.application.port.in;

public interface UserCleanupUseCase {
  int hardDeleteSoftDeletedUsersOlderThanOneDay();
}
