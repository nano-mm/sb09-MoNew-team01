package com.monew.application.port.in;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserActivityDto;
import com.monew.dto.response.UserDto;
import java.util.UUID;

public interface UserUseCase {
  UserDto create(UserRegisterRequest request);
  UserDto login(UserLoginRequest request);
  UserDto update(UUID userId, UserUpdateRequest request);
  UserActivityDto getActivity(UUID userId);
  void softDelete(UUID userId);
  void hardDelete(UUID userId);
}
