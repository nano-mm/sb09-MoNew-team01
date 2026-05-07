package com.monew.application.port.in;

import com.monew.dto.response.UserActivityDto;
import java.util.UUID;

public interface UserActivityQueryUseCase {
  UserActivityDto getActivity(UUID userId);
}
