package com.monew.repository;

import com.monew.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdAndCreatedAtLessThanOrderByCreatedAtDesc(
      UUID userId,
      Instant cursor,
      Pageable pageable
  );

  List<Notification> findByUserIdOrderByCreatedAtDesc(
      UUID userId,
      Pageable pageable
  );

  default List<Notification> findByUserIdWithCursor(UUID userId, UUID cursorId, int size) {
    int pageSize = Math.max(1, size);
    Pageable pageable = PageRequest.of(0, pageSize);

    if (cursorId == null) {
      return findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    return findById(cursorId)
        .map(Notification::getCreatedAt)
        .map(cursor -> findByUserIdAndCreatedAtLessThanOrderByCreatedAtDesc(userId, cursor, pageable))
        .orElse(List.of());
  }

  long countByUserId(UUID userId);
}