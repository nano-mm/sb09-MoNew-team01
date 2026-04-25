package com.monew.repository;

import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import com.monew.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  @EntityGraph(attributePaths = {"interest"})
  @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId")
  List<Subscription> findAllByUserIdWithInterest(@Param("userId") UUID userId);

  @Query("SELECT DISTINCT s.user.id FROM Subscription s WHERE s.interest.id = :interestId")
  List<UUID> findDistinctUserIdsByInterestId(@Param("interestId") UUID interestId);

  boolean existsByUserAndInterest(User user, Interest interest);

  Optional<Subscription> findByUserAndInterest(User user, Interest interest);

  long countByInterest(Interest interest);

  void deleteByInterestId(UUID interestId);
}