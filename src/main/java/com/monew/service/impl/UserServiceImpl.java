package com.monew.service.impl;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import com.monew.mapper.UserMapper;
import com.monew.repository.UserRepository;
import com.monew.service.UserService;
import java.util.UUID;
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

  @Override
  public UserDto login(UserLoginRequest request) {
    String email = request.email();
    String password = request.password();
    return userMapper.toDto(validate(email, password));
  }

  @Override
  public UserDto update(UUID userId, UserUpdateRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    userRepository.save(user.update(request.nickname()));
    return userMapper.toDto(user);
  }

  private User validate(String email, String password) {
    User user =  userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(
        "Wrong email or password"));
    if(user.getPassword().equals(password)){
      return user;
    } else {
      throw new IllegalArgumentException("Wrong email or password");
    }
  }
}
