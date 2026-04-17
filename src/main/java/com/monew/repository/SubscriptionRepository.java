package com.monew.repository;

import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  List<Subscription> findByUserId(UUID userId);

  boolean existsByUserIdAndInterest(UUID userId, Interest interest);
}