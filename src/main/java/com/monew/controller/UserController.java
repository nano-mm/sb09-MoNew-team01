package com.monew.controller;

import com.monew.config.LoginUser;
import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.exception.user.UnauthorizedException;
import com.monew.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  // 회원가입
  @PostMapping
  public ResponseEntity<UserDto> create(@RequestBody @Valid UserRegisterRequest request){
    log.debug("사용자 생성 시도. 사용자 email: {}", request.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
  }

  @PostMapping("login")
  public ResponseEntity<UserDto> login(@RequestBody @Valid UserLoginRequest request){
    log.debug("사용자 로그인 시도. 사용자 email: {}", request.email());
    return ResponseEntity.ok().body(userService.login(request));
  }

  @PatchMapping("{userId}")
  public ResponseEntity<UserDto> update(@LoginUser UUID loginUserId, @PathVariable UUID userId, @RequestBody @Valid UserUpdateRequest request) {
    log.debug("사용자 업데이트 시도. 요청 id: {}", loginUserId);
    return ResponseEntity.ok().body(userService.update(userId, request));
  }

  @DeleteMapping("{userId}")
  public ResponseEntity<Void> logicalDelete(@LoginUser UUID loginUserId, @PathVariable UUID userId) {
    log.debug("사용자 논리적 삭제 시도. 요청 id: {}", loginUserId);
    if (!loginUserId.equals(userId)) {
      log.warn("사용자 논리적 삭제 권한 없음. 요청 id: {}, 대상 id: {}", loginUserId, userId);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    userService.softDelete(userId);
    log.info("사용자 논리적 삭제 완료. userId: {}", userId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("{userId}/hard")
  public ResponseEntity<Void> hardDelete(@PathVariable UUID userId) {
    log.warn("사용자 물리적 삭제 시도. 요청 id: {}", userId);
    userService.hardDelete(userId);
    return ResponseEntity.noContent().build();
  }
}
