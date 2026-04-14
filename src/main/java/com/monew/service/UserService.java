package com.monew.service;

import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.response.UserDto;

public interface UserService {

  UserDto create(UserRegisterRequest request);
}
