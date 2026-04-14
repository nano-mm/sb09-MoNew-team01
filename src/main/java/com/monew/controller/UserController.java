package com.monew.controller;

import com.monew.dto.request.UserRegisterRequest;
import com.monew.dto.response.UserDto;
import com.monew.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
