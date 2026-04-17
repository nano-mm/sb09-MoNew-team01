package com.monew.repository;

import com.monew.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdAndConfirmedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(
      UUID userId,
      Instant cursor,
      Pageable pageable
  );

  List<Notification> findByUserIdAndConfirmedFalseOrderByCreatedAtDesc(
      UUID userId,
      Pageable pageable
  );

  default List<Notification> findByUserIdWithCursor(UUID userId, Instant cursorPoint, int size) {
    int pageSize = Math.max(1, size);
    Pageable pageable = PageRequest.of(0, pageSize);

    if (cursorPoint == null) {
      return findByUserIdAndConfirmedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    return findByUserIdAndConfirmedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(userId, cursorPoint, pageable);
  }

  Optional<Notification> findByIdAndUserIdAndConfirmedFalse(UUID id, UUID userId);

  List<Notification> findAllByUserIdAndConfirmedFalse(UUID userId);

  long countByUserIdAndConfirmedFalse(UUID userId);
}