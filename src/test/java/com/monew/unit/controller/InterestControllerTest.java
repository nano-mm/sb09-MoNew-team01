package com.monew.unit.controller;

import com.monew.config.SecurityConfig;
import com.monew.controller.InterestController;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.repository.UserRepository;
import com.monew.service.InterestService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InterestController.class)
@Import(SecurityConfig.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InterestService interestService;

  // LoginUserArgumentResolver 때문에 필요
  @MockitoBean
  private UserRepository userRepository;

  private final UUID INTEREST_ID = UUID.randomUUID();
  private final UUID USER_ID = UUID.randomUUID();

  @Test
  @DisplayName("관심사 생성 - 성공")
  @WithMockUser
  void create_Success() throws Exception {

    Interest mockInterest = new Interest("스포츠", List.of("축구"));

    given(interestService.create(any())).willReturn(mockInterest);

    mockMvc.perform(post("/api/interests")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "name": "스포츠",
                  "keywords": ["축구"]
                }
                """))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("관심사 수정 - 성공")
  @WithMockUser
  void update_Success() throws Exception {

    mockMvc.perform(patch("/api/interests/{id}", INTEREST_ID)
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "keywords": ["야구"]
                }
                """))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("관심사 삭제 - 성공")
  @WithMockUser
  void delete_Success() throws Exception {

    mockMvc.perform(delete("/api/interests/{id}", INTEREST_ID))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("관심사 목록 조회 - 성공")
  @WithMockUser
  void find_Success() throws Exception {

    InterestDto dto = new InterestDto(
        INTEREST_ID,
        "스포츠",
        List.of("축구"),
        10L,
        false
    );

    CursorPageResponseDto<InterestDto> mockResponse =
        CursorPageResponseDto.<InterestDto>builder()
            .content(List.of(dto))
            .nextCursor(null)
            .nextAfter(null)
            .size(1)
            .hasNext(false)
            .build();

    given(interestService.find(any())).willReturn(mockResponse);

    mockMvc.perform(get("/api/interests")
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("스포츠"))
        .andExpect(jsonPath("$.content[0].subscriberCount").value(10))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("관심사 목록 조회 - 실패 (size 타입 오류)")
  @WithMockUser
  void find_Fail_TypeMismatch() throws Exception {

    mockMvc.perform(get("/api/interests")
            .param("size", "ten"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 구독 - 성공")
  @WithMockUser
  void subscribe_Success() throws Exception {

    mockMvc.perform(post("/api/interests/{id}/subscriptions", INTEREST_ID))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("관심사 구독 취소 - 성공")
  @WithMockUser
  void unsubscribe_Success() throws Exception {

    mockMvc.perform(delete("/api/interests/{id}/subscriptions", INTEREST_ID))
        .andExpect(status().isOk());
  }
}