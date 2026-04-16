package com.monew.controller;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.service.NotificationService;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<NotificationDto>> getNotifications(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam @Min(1) int limit,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    return ResponseEntity.ok(notificationService.getNotifications(userId, cursor, after, limit));
  }
}
