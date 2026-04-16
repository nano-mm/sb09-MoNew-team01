package com.monew.repository;

import com.monew.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query("""
      select n
      from Notification n
      where n.userId = :userId
        and n.confirmed = false
        and (
          :cursorAfter is null
          or n.createdAt < :cursorAfter
          or (n.createdAt = :cursorAfter and (:cursorId is null or n.id < :cursorId))
        )
      order by n.createdAt desc, n.id desc
      """)
  List<Notification> findByUserIdWithCursor(
      UUID userId,
      Instant cursorAfter,
      UUID cursorId,
      Pageable pageable
  );

  default List<Notification> findByUserIdWithCursor(UUID userId, Instant cursorAfter, UUID cursorId, int size) {
    int pageSize = Math.max(1, size);
    Pageable pageable = PageRequest.of(0, pageSize);

    return findByUserIdWithCursor(userId, cursorAfter, cursorId, pageable);
  }

  java.util.Optional<Notification> findByIdAndUserIdAndConfirmedFalse(UUID id, UUID userId);

  long countByUserIdAndConfirmedFalse(UUID userId);
}