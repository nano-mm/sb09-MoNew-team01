package com.monew.unit.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.config.LoginUserArgumentResolver;
import com.monew.config.WebConfig;
import com.monew.adapter.in.web.UserActivityController;
import com.monew.dto.response.UserActivityDto;
import com.monew.exception.GlobalExceptionHandler;
import com.monew.adapter.out.persistence.UserRepository;
import com.monew.application.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({UserActivityController.class, WebConfig.class, LoginUserArgumentResolver.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class UserActivityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  @DisplayName("요청에 대한 응답을 제공한다")
  void getActivity() throws Exception {
    UUID userId = UUID.randomUUID();
    UserActivityDto response = UserActivityDto.builder()
        .id(userId)
        .email("test@test.com")
        .nickname("Tester")
        .createdAt(Instant.now())
        .subscriptions(Collections.emptyList())
        .comments(Collections.emptyList())
        .commentLikes(Collections.emptyList())
        .articleViews(Collections.emptyList())
        .build();

    // SecurityContext에 인증 정보 설정 (LoginUserArgumentResolver가 사용함)
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    when(userService.getActivity(userId)).thenReturn(response);

    mockMvc.perform(get("/api/user-activities/{userId}", userId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()));
  }
}
