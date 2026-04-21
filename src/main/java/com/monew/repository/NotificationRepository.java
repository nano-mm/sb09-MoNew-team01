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

  List<Notification> findByUser_IdAndConfirmedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(
      UUID userId,
      Instant cursor,
      Pageable pageable
  );

  List<Notification> findByUser_IdAndConfirmedFalseOrderByCreatedAtDesc(
      UUID userId,
      Pageable pageable
  );

  default List<Notification> findByUserIdWithCursor(UUID userId, Instant cursorPoint, int size) {
    int pageSize = Math.max(1, size);
    Pageable pageable = PageRequest.of(0, pageSize);

    if (cursorPoint == null) {
      return findByUser_IdAndConfirmedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    return findByUser_IdAndConfirmedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(userId, cursorPoint, pageable);
  }

  Optional<Notification> findByIdAndUser_IdAndConfirmedFalse(UUID id, UUID userId);

  List<Notification> findAllByUser_IdAndConfirmedFalse(UUID userId);

  long countByUser_IdAndConfirmedFalse(UUID userId);

  long deleteByConfirmedIsTrueAndCreatedAtBefore(Instant threshold);
}