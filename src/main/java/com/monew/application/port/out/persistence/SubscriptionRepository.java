package com.monew.application.port.out.persistence;

import com.monew.domain.model.Interest;
import com.monew.domain.model.Subscription;
import com.monew.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  @EntityGraph(attributePaths = {"interest"})
  List<Subscription> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);

  @Query("SELECT DISTINCT s.user.id FROM Subscription s WHERE s.interest.id = :interestId")
  List<UUID> findDistinctUserIdsByInterestId(@Param("interestId") UUID interestId);

  boolean existsByUserAndInterest(User user, Interest interest);

  Optional<Subscription> findByUserAndInterest(User user, Interest interest);

  List<Subscription> findAllByUserAndInterest(User user, Interest interest);

  long countByInterest(Interest interest);

  void deleteByInterestId(UUID interestId);

  @Query("select s.user.id from Subscription s where s.interest.id = :interestId")
  List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);
}