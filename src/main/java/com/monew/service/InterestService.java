package com.monew.service;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import java.util.UUID;

public interface InterestService {

  Interest create(InterestRegisterRequest request);

  void update(UUID id, InterestUpdateRequest request);

  void delete(UUID id);

  CursorPageResponseDto<InterestDto> find(InterestSearchRequest request);

  void subscribe(UUID userId, UUID interestId);

  void unsubscribe(UUID userId, UUID interestId);
}