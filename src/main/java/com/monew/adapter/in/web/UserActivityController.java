package com.monew.adapter.in.web;

import com.monew.dto.response.UserActivityDto;
import com.monew.application.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user-activities")
public class UserActivityController {

  private final UserService userService;

  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityDto> getActivity(@PathVariable UUID userId) {
    log.debug("사용자 활동 내역 조회 시도. 요청 id: {}", userId);
    UserActivityDto response = userService.getActivity(userId);
    log.debug("사용자 활동 내역 조회 성공. 요청 id: {}", userId);
    return ResponseEntity.ok(response);
  }
}
