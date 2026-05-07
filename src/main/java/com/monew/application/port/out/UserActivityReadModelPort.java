package com.monew.application.port.out;

import com.monew.dto.response.UserActivityDto;
import java.util.Optional;
import java.util.UUID;

public interface UserActivityReadModelPort {
  boolean isEnabled();
  Optional<UserActivityDto> findByUserId(UUID userId);
  void refreshSnapshot(UUID userId);
  void deleteSnapshot(UUID userId);
  void removeSubscriptionSnapshot(UUID userId, UUID interestId);
  void refreshSnapshotsForInterestSubscribers(UUID interestId);
}
