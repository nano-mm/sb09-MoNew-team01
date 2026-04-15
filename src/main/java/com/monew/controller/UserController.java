package com.monew.controller;

import com.monew.dto.request.UserLoginRequest;
import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.request.UserUpdateRequest;
import com.monew.dto.response.UserDto;
import com.monew.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/api/users")
public class UserController {
  private final UserService userService;

  // 회원가입
  @PostMapping
  public ResponseEntity<UserDto> create(@RequestBody @Valid UserRegisterRequest request){
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
  }

  @PostMapping("login")
  public ResponseEntity<UserDto> login(@RequestBody @Valid UserLoginRequest request){
    return ResponseEntity.ok().body(userService.login(request));
  }

  @PatchMapping("{userId}")
  public ResponseEntity<UserDto> update(@PathVariable UUID userId, @RequestBody @Valid UserUpdateRequest request) {
    return ResponseEntity.ok().body(userService.update(userId, request));
  }
}
