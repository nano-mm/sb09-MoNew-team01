package com.monew.adapter.out.persistence;

import com.monew.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByEmail(String email);

  boolean existsById(UUID id);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :threshold")
  int deleteSoftDeletedUsersOlderThan(Instant threshold);
}
