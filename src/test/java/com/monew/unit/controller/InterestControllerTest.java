package com.monew.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.config.LoginUser;
import com.monew.config.SecurityConfig;
import com.monew.adapter.in.web.InterestController;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.domain.model.Interest;
import com.monew.adapter.out.persistence.UserRepository;
import com.monew.application.service.InterestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InterestController.class)
@Import(SecurityConfig.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @MockitoBean
  private UserRepository userRepository;

  private final UUID interestId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final String headUserId = "Monew-Request-User-ID";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    InterestController controller = new InterestController(interestService);

    mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(LoginUser.class);
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory
          ) {
            return userId; // 테스트용 userId 주입
          }
        })
        .build();
  }

  @Test
  @DisplayName("관심사 생성 - 성공")
  @WithMockUser
  void create_Success() throws Exception {

    InterestRegisterRequest request = new InterestRegisterRequest("IT", List.of("AI"));

    Interest interest = new Interest("IT", List.of("AI"));

    given(interestService.create(request)).willReturn(interest);

    mockMvc.perform(post("/api/interests")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    verify(interestService).create(request);
  }

  @Test
  @DisplayName("관심사 수정 - 성공")
  @WithMockUser
  void update_Success() throws Exception {

    InterestUpdateRequest request = new InterestUpdateRequest(List.of("AI"));

    mockMvc.perform(patch("/api/interests/{id}", interestId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(interestService).update(interestId, request);
  }

  @Test
  @DisplayName("관심사 삭제 - 성공")
  @WithMockUser
  void delete_Success() throws Exception {

    mockMvc.perform(delete("/api/interests/{id}", interestId))
        .andExpect(status().isOk());

    verify(interestService).delete(interestId);
  }

  @Test
  @DisplayName("관심사 목록 조회 - 성공")
  @WithMockUser
  void find_Success() throws Exception {

    CursorPageResponseDto<InterestDto> mockResponse =
        CursorPageResponseDto.<InterestDto>builder()
            .content(List.of())
            .hasNext(false)
            .build();

    given(interestService.find(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    )).willReturn(mockResponse);

    mockMvc.perform(get("/api/interests")
            .param("keyword", "IT")
            .param("limit", "10")
            .param("orderBy", "name")
            .param("direction", "ASC"))
        .andExpect(status().isOk());

    verify(interestService).find(
        org.mockito.ArgumentMatchers.eq("IT"),
        org.mockito.ArgumentMatchers.any(CursorRequest.class),
        org.mockito.ArgumentMatchers.eq(userId)
    );

  }

  @Test
  @DisplayName("관심사 목록 조회 - 실패(타입 오류)")
  @WithMockUser
  void find_Fail_TypeMismatch() throws Exception {

    mockMvc.perform(get("/api/interests")
            .header(headUserId, userId.toString())
            .param("limit", "ten")) //일부러 잘못된 타입 삽입
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 목록 조회 - 실패 (필수 파라미터 누락)")
  void find_Fail_MissingParams() throws Exception {

    mockMvc.perform(get("/api/interests")
            .param("keyword", "IT")) // limit, orderBy, direction 없는 경우
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("관심사 구독 - 성공")
  @WithMockUser
  void subscribe_Success() throws Exception {

    mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
            .header(headUserId, userId.toString()))
        .andExpect(status().isCreated());

    verify(interestService).subscribe(userId, interestId);
  }

  @Test
  @DisplayName("관심사 구독 취소 - 성공")
  @WithMockUser
  void unsubscribe_Success() throws Exception {

    mockMvc.perform(delete("/api/interests/{id}/subscriptions", interestId)
            .header(headUserId, userId.toString()))
        .andExpect(status().isNoContent());

    verify(interestService).unsubscribe(userId, interestId);
  }
}