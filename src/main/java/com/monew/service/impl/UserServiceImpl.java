package com.monew.service.impl;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import com.monew.exception.user.DuplicateEmailException;
import com.monew.exception.user.InvalidPasswordException;
import com.monew.exception.user.PasswordPatternException;
import com.monew.exception.user.UserNotFoundException;
import com.monew.mapper.UserMapper;
import com.monew.repository.UserRepository;
import com.monew.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final PasswordEncoder passwordEncoder;
  private final EntityManager entityManager;
  private final JdbcTemplate jdbcTemplate;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public UserDto create(UserRegisterRequest request){
    if(existsInAllUsers(request.email())) {
      log.warn("중복된 이메일 오류: {}", request.email());
      throw new DuplicateEmailException("Email already exists");
    }

    String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{6,20}$";

    if(!request.password().matches(regex)) {
      throw new PasswordPatternException("영문과 숫자, 특수문자를 포함해 6자 이상 입력해 주세요");
    }

    String encodedPassword = passwordEncoder.encode(request.password());

    User user = User.of(request.email(), request.nickname(), encodedPassword);
    User savedUser = userRepository.save(user);
    log.info("사용자 생성 완료. userId: {}", user.getId());
    return userMapper.toDto(savedUser);
  }

  @Override
  public UserDto login(UserLoginRequest request) {
    String email = request.email();
    String password = request.password();
    User user = loginValidate(email, password);
    log.info("사용자 로그인 성공. userId: {}", user.getId());
    return userMapper.toDto(user);
  }

  @Override
  public UserDto update(UUID userId, UserUpdateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자 업데이트 실패. 존재하지 않는 사용자 id: {}", userId);
          return new UserNotFoundException("User not found with id: " + userId);
        });
    user.update(request.nickname());
    log.info("사용자 닉네임 변경 성공. 사용자 id: {}", user.getId());
    return userMapper.toDto(user);
  }

  @Override
  public void softDelete(UUID userId) {
    User user = userRepository.findById(userId).orElseThrow(
        () -> {
          log.warn("softDelete 실패. 존재하지 않는 사용자 id: {}", userId);
          return new UserNotFoundException("User not found with id: " + userId);
        }
    );

    user.markAsDeleted(true);
  }

  @Override
  public void hardDelete(UUID userId) {
    String checkSql = "SELECT COUNT(*) FROM users WHERE id = CAST(? AS UUID)";
    Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);

    if (count == null || count == 0) {
      log.warn("hardDelete 실패. DB에 존재하지 않는 사용자 id: {}", userId);
      throw new UserNotFoundException("User not found with id: " + userId);
    }

    entityManager.flush();
    entityManager.clear();
    String deleteSql = "DELETE FROM users WHERE id = CAST(? AS UUID)";
    jdbcTemplate.update(deleteSql, userId);
    log.warn("HardDelete 성공. 사용자 id: {}", userId);
  }

  private User loginValidate(String email, String password) {
    User user =  userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("로그인 실패. 존재하지 않는 사용자 email: {}", email);
          return new UserNotFoundException("Wrong email or password");
        });
    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("사용자 비밀번호 검증 실패. 사용자 id: {}", user.getId());
      throw new InvalidPasswordException("Wrong email or password");
    }

    return user;
  }

  private boolean existsInAllUsers(String email) {
    String sql = "SELECT count(*) FROM users WHERE email = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
    return count != null && count > 0;
  }
}
