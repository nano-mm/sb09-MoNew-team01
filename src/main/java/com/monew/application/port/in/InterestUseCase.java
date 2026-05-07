package com.monew.application.port.in;

import com.monew.domain.model.Interest;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.dto.response.SubscriptionDto;
import java.util.UUID;

public interface InterestUseCase {
  Interest create(InterestRegisterRequest request);
  void update(UUID id, InterestUpdateRequest request);
  void delete(UUID id);
  CursorPageResponseDto<InterestDto> find(String keyword, CursorRequest cursorRequest, UUID userId);
  SubscriptionDto subscribe(UUID userId, UUID interestId);
  void unsubscribe(UUID userId, UUID interestId);
}
