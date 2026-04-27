package com.monew.unit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.config.LoginUserArgumentResolver;
import com.monew.config.SecurityConfig;
import com.monew.controller.UserController;
import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.exception.user.InvalidPasswordException;
import com.monew.exception.user.UserNotFoundException;
import com.monew.repository.UserRepository;
import com.monew.service.UserService;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(SecurityConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private UserService userService;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private LoginUserArgumentResolver loginUserArgumentResolver;

  private final UUID SAME_USER_ID = UUID.randomUUID();
  private final UUID DIFFERENT_USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() throws BadRequestException {
    when(loginUserArgumentResolver.supportsParameter(any())).thenReturn(true);
    when(loginUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
        .thenReturn(SAME_USER_ID);
  }

  @Test
  @DisplayName("회원가입 API 성공 - 201 Created")
  void register_ApiSuccess() throws Exception {
    /// given
    UserRegisterRequest request = new UserRegisterRequest("test@test.com", "Tester", "Password123!");
    UserDto response = new UserDto(UUID.randomUUID(), "test@test.com", "Tester", Instant.now());

    /// when
    when(userService.create(any())).thenReturn(response);

    /// then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Nested
  @DisplayName("로그인 API Test")
  class UserLoginTests {
    @Test
    @DisplayName("로그인 API 성공 - 200 OK")
    void login_ApiSuccess() throws Exception {
      /// given & when
      UserLoginRequest request = new UserLoginRequest("test@test.com", "Password123!");
      when(userService.login(any())).thenReturn(new UserDto(UUID.randomUUID(), "test@test.com", "Tester", Instant.now()));

      /// then
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("요청 ID/PW가 맞지 않음")
    void login_ApiFailed_invalid() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest("test@test.com", "Password123!");

      when(userService.login(any()))
          .thenThrow(new NoSuchElementException("Wrong email or password"));

      // when & then
      mockMvc.perform(post("/api/users/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(result -> {
            assertTrue(result.getResolvedException() instanceof NoSuchElementException);
            assertEquals("Wrong email or password", result.getResolvedException().getMessage());
          });
    }
  }

  @Nested
  @DisplayName("삭제 API Test")
  class UserDeleteTests {
    @Test
    @DisplayName("성공: 본인 계정 논리적 삭제 시 204 No Content")
    void logicalDelete_Success() throws Exception {
      /// given&when
      mockMvc.perform(delete("/api/users/{userId}", SAME_USER_ID)
              .with(csrf()))
          .andExpect(status().isNoContent());

      /// then
      verify(userService, times(1)).softDelete(SAME_USER_ID);
    }

    @Test
    @DisplayName("성공: 물리적 삭제(Hard Delete) 시 204 No Content")
    void hardDelete_Success() throws Exception {
      /// given&when
      mockMvc.perform(delete("/api/users/{userId}/hard", SAME_USER_ID)
              .with(csrf()))
          .andExpect(status().isNoContent());

      /// then
      verify(userService, times(1)).hardDelete(SAME_USER_ID);
    }
  }

  @Nested
  @DisplayName("업데이트 API Test")
  class UserUpdateTests {
    @Test
    @DisplayName("성공: 본인 정보 수정 시 200 OK")
    void update_ApiSuccess() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("NewNickname");
      UserDto response = new UserDto(SAME_USER_ID, "test@test.com", "NewNickname", Instant.now());
      when(userService.update(eq(SAME_USER_ID), any())).thenReturn(response);

      // when & then
      mockMvc.perform(patch("/api/users/{userId}", SAME_USER_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("NewNickname"));
    }
  }
}