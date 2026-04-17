package com.monew.repository;

import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import com.monew.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByUserAndInterest(User user, Interest interest);

  Optional<Subscription> findByUserAndInterest(User user, Interest interest);

  long countByInterest(Interest interest);
}