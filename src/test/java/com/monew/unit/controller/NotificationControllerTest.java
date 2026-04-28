package com.monew.unit.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.repository.UserRepository;
import com.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(com.monew.controller.NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  void getNotifications_정상요청이면_200을_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(),
        Instant.parse("2026-04-19T10:00:00Z"),
        Instant.parse("2026-04-19T10:00:00Z"),
        false,
        userId,
        "알림 내용",
        "interest",
        UUID.randomUUID()
    );

    CursorPageResponseDto<NotificationDto> response = new CursorPageResponseDto<>(
        List.of(dto),
        dto.id().toString(),
        dto.createdAt(),
        1,
        1L,
        false
    );

    when(notificationService.getNotifications(eq(userId), isNull(), isNull(), eq(10)))
        .thenReturn(response);

    mockMvc.perform(get("/api/notifications")
            .param("limit", "10")
            .header("Monew-Request-User-ID", userId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  void getNotifications_limit이_0이면_400을_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(get("/api/notifications")
            .param("limit", "0")
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_400"));
  }

  @Test
  void getNotifications_헤더가_없으면_400을_반환한다() throws Exception {
    mockMvc.perform(get("/api/notifications").param("limit", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_400"));
  }

  @Test
  void confirmNotification_정상요청이면_200을_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isOk());

    verify(notificationService).confirmNotification(userId, notificationId);
  }

  @Test
  void confirmNotification_사용자가_없으면_404를_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    doThrow(new BaseException(ErrorCode.USER_NOT_FOUND))
        .when(notificationService)
        .confirmNotification(userId, notificationId);

    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_404"));
  }

  @Test
  void confirmAllNotifications_정상요청이면_200을_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(patch("/api/notifications")
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isOk());

    verify(notificationService).confirmAllNotifications(userId);
  }

  @Test
  void confirmAllNotifications_사용자가_없으면_404를_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();

    doThrow(new BaseException(ErrorCode.USER_NOT_FOUND))
        .when(notificationService)
        .confirmAllNotifications(userId);

    mockMvc.perform(patch("/api/notifications")
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_404"));
  }
}

