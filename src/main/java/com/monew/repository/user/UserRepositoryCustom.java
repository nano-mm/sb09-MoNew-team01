package com.monew.repository.user;

import java.time.Instant;
import java.util.UUID;

public interface UserRepositoryCustom {
  int deleteSoftDeletedUsersOlderThan(Instant dateTime);

  boolean existsInAllUsers(String email);

  boolean existsByIdPhysical(UUID userId);

  void deleteByIdPhysical(UUID userId);
}
