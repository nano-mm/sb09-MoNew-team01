package com.monew.service;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.dto.response.UserActivityResponse;
import jakarta.validation.Valid;
import java.util.UUID;

public interface UserService {

  UserDto create(UserRegisterRequest request);

  UserDto login(@Valid UserLoginRequest request);

  UserDto update(UUID userId, UserUpdateRequest request);

  void softDelete(UUID userId);

  void hardDelete(UUID userId);

  UserActivityResponse getActivity(UUID userId);
}
