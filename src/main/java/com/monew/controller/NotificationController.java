package com.monew.controller;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.service.NotificationService;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<NotificationDto>> getNotifications(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "10") @Min(1) int limit,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    log.debug(
        "[알림] 목록 조회 요청. userId={}, limit={}, cursor존재={}, after존재={}",
        userId,
        limit,
        cursor != null && !cursor.isBlank(),
        after != null
    );

    CursorPageResponseDto<NotificationDto> response = notificationService.getNotifications(
        userId,
        cursor,
        after,
        limit
    );

    log.debug(
        "[알림] 목록 조회 완료. userId={}, 반환개수={}, 다음페이지존재={}",
        userId,
        response.size(),
        response.hasNext()
    );

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{notificationId}")
  public ResponseEntity<Void> confirmNotification(
      @PathVariable UUID notificationId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    log.info("[알림] 단건 확인 요청. userId={}, notificationId={}", userId, notificationId);
    notificationService.confirmNotification(userId, notificationId);
    log.info("[알림] 단건 확인 완료. userId={}, notificationId={}", userId, notificationId);
    return ResponseEntity.ok().build();
  }

  @PatchMapping
  public ResponseEntity<Void> confirmAllNotifications(
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    log.info("[알림] 전체 확인 요청. userId={}", userId);
    notificationService.confirmAllNotifications(userId);
    log.info("[알림] 전체 확인 완료. userId={}", userId);
    return ResponseEntity.ok().build();
  }
}
