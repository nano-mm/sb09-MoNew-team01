package com.monew.repository;

import com.monew.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
}