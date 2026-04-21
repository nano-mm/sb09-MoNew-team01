package com.monew.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import com.monew.exception.user.*;
import com.monew.mapper.UserMapper;
import com.monew.repository.UserRepository;
import com.monew.service.impl.UserServiceImpl;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @Mock private EntityManager entityManager;

  @InjectMocks
  private UserServiceImpl userService;

  @Nested
  @DisplayName("회원가입 테스트")
  class RegisterTest {
    @Test
    @DisplayName("성공: 올바른 정보로 회원가입")
    void create_Success() {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@test.com", "Tester", "Password123!");
      when(userRepository.existsInAllUsers(anyString())).thenReturn(false);
      when(passwordEncoder.encode(any())).thenReturn("encoded_pw");
      when(userRepository.save(any())).thenReturn(User.of(request.email(), request.nickname(), "encoded_pw"));
      when(userMapper.toDto(any())).thenReturn(new UserDto(UUID.randomUUID(), request.email(), request.nickname(), Instant.now()));

      // when
      UserDto result = userService.create(request);

      // then
      assertNotNull(result);
      verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("실패: 이메일 중복")
    void create_DuplicateEmail() {
      UserRegisterRequest request = new UserRegisterRequest("exist@test.com", "Nick", "Pass123!");
      when(userRepository.existsInAllUsers(anyString())).thenReturn(true);

      assertThrows(AlreadyExistEmailException.class, () -> userService.create(request));
    }

    @Test
    @DisplayName("실패: 비밀번호 패턴 부적합")
    void create_InvalidPasswordPattern() {
      UserRegisterRequest request = new UserRegisterRequest("test@test.com", "Nick", "123");
      when(userRepository.existsInAllUsers(anyString())).thenReturn(false);

      assertThrows(PasswordPatternException.class, () -> userService.create(request));
    }
  }

  @Nested
  @DisplayName("로그인 테스트")
  class LoginTest {
    @Test
    @DisplayName("성공: 이메일과 비밀번호 일치")
    void login_Success() {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "Pass123!");
      User user = User.of("test@test.com", "Tester", "encoded_pw");

      when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(any(), any())).thenReturn(true);
      when(userMapper.toDto(any())).thenReturn(new UserDto(UUID.randomUUID(), "test@test.com", "Tester", Instant.now()));

      UserDto result = userService.login(request);
      assertNotNull(result);
    }

    @Test
    @DisplayName("실패: 잘못된 비밀번호")
    void login_InvalidPassword() {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "WrongPass");
      User user = User.of("test@test.com", "Tester", "encoded_pw");

      when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(any(), any())).thenReturn(false);

      assertThrows(IllegalArgumentException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 이메일")
    void login_UserNotFound() {
      UserLoginRequest request = new UserLoginRequest("non-exist@test.com", "Pass123!");
      when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

      assertThrows(IllegalArgumentException.class, () -> userService.login(request));
    }
  }

  @Test
  @DisplayName("회원정보 수정 성공")
  void update_Success() {
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("NewNick");
    User user = User.of("test@test.com", "OldNick", "pw");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(any())).thenReturn(new UserDto(userId, "test@test.com", "NewNick", Instant.now()));

    UserDto result = userService.update(userId, request);
    assertEquals("NewNick", result.nickname());
  }

  @Nested
  @DisplayName("삭제 테스트")
  class DeleteTest {

    @Test
    @DisplayName("Soft Delete 성공")
    void softDelete_Success() {
      UUID userId = UUID.randomUUID();
      User user = spy(User.of("test@test.com", "Tester", "pw"));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      userService.softDelete(userId);

      verify(user).markAsDeleted(true);
    }

    @Test
    @DisplayName("hardDelete 성공: 사용자가 존재하면 Repository를 통해 삭제한다")
    void hardDelete_Success() {
      // given
      UUID userId = UUID.randomUUID();
      when(userRepository.existsByIdPhysical(userId)).thenReturn(true);

      // when
      userService.hardDelete(userId);

      // then
      verify(entityManager, times(1)).flush();
      verify(entityManager, times(1)).clear();
      verify(userRepository, times(1)).deleteByIdPhysical(userId);
    }

    @Test
    @DisplayName("Hard Delete 실패: 사용자를 찾을 수 없음")
    void hardDelete_Fail_NotFound() {
      // given
      UUID userId = UUID.randomUUID();
      when(userRepository.existsByIdPhysical(userId)).thenReturn(false);

      // when & then
      assertThrows(NoSuchElementException.class, () -> userService.hardDelete(userId));
    }
  }
}