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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final JdbcTemplate jdbcTemplate;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public UserDto create(UserRegisterRequest request){
    if(existsInAllUsers(request.email()))
      throw new RuntimeException("Email already exists");
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
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new  UserNotFoundException("User not found with id: " + userId));
    user.update(request.nickname());
    return userMapper.toDto(user);
  }

  @Override
  public void softDelete(UUID userId) {
    User user = userRepository.findById(userId).orElseThrow();
    userRepository.delete(user);
  }

  @Override
  public void hardDelete(UUID userId) {
    String sql = "DELETE FROM users WHERE id = ?";
    jdbcTemplate.update(sql, userId);
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

  private boolean existsInAllUsers(String email) {
    String sql = "SELECT count(*) FROM users WHERE email = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
    return count != null && count > 0;
  }
}
