package com.monew.controller;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.mapper.InterestMapper;
import com.monew.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심사 관리", description = "관심사 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController {

  private final InterestService interestService;

  // 1. 관심사 등록

  @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
  @PostMapping
  public ResponseEntity<InterestDto> create(@Valid @RequestBody InterestRegisterRequest request) {
    Interest interest = interestService.create(request);
    InterestDto response = InterestMapper.toDto(interest,false);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}