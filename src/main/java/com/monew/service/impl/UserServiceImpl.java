package com.monew.service.impl;

import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import com.monew.mapper.UserMapper;
import com.monew.repository.UserRepository;
import com.monew.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public UserDto create(UserRegisterRequest request){
    User user = User.to(request.email(), request.nickname(), request.password());
    User savedUser = userRepository.save(user);
    return userMapper.toDto(savedUser);
  }
}
