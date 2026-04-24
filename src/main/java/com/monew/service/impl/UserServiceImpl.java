package com.monew.service.impl;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserActivityDto;
import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import com.monew.exception.user.AlreadyExistEmailException;
import com.monew.exception.user.PasswordPatternException;
import com.monew.mapper.UserMapper;
import com.monew.repository.UserRepository;
import com.monew.service.UserActivityDtoBuilder;
import com.monew.service.UserActivityReadModelService;
import com.monew.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  private final UserActivityDtoBuilder userActivityDtoBuilder;
  private final UserActivityReadModelService userActivityReadModelService;

  @Override
  public UserDto create(UserRegisterRequest request){
    if(existsInAllUsers(request.email())) {
      log.warn("중복된 이메일 오류: {}", request.email());
      throw new AlreadyExistEmailException("Email already exists");
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
          return new NoSuchElementException("User not found with id: " + userId);
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
          return new NoSuchElementException("User not found with id: " + userId);
        }
    );

    user.markAsDeleted(true);
  }

  @Override
  public void hardDelete(UUID userId) {
    if (!userRepository.existsByIdPhysical(userId)) {
      log.warn("hardDelete 실패. DB에 존재하지 않는 사용자 id: {}", userId);
      throw new NoSuchElementException("User not found with id: " + userId);
    }

    entityManager.flush();
    entityManager.clear();
    userActivityReadModelService.deleteSnapshot(userId);
    userRepository.deleteByIdPhysical(userId);
    log.warn("HardDelete 성공. 사용자 id: {}", userId);
  }

  private User loginValidate(String email, String password) {
    User user =  userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("로그인 실패. 존재하지 않는 사용자 email: {}", email);
          return new IllegalArgumentException("Wrong email or password");
        });
    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("사용자 비밀번호 검증 실패. 사용자 id: {}", user.getId());
      throw new IllegalArgumentException("Wrong email or password");
    }

    return user;
  }

  private boolean existsInAllUsers(String email) {
    return userRepository.existsInAllUsers(email);
  }

  @Override
  @Transactional(readOnly = true)
  public UserActivityDto getActivity(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new NoSuchElementException("User not found with id: " + userId);
    }

    if (userActivityReadModelService.isEnabled()) {
      return userActivityReadModelService.findByUserId(userId)
          .orElseGet(() -> {
            userActivityReadModelService.refreshSnapshot(userId);
            return userActivityReadModelService.findByUserId(userId)
                .orElseGet(() -> userActivityDtoBuilder.build(userId));
          });
    }

    return userActivityDtoBuilder.build(userId);
  }
}
