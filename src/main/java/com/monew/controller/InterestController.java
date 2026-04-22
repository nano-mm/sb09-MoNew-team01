package com.monew.controller;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.mapper.InterestMapper;
import com.monew.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심사 관리", description = "관심사 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController {

  private final InterestService interestService;

  @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
  @PostMapping
  public ResponseEntity<InterestDto> create(@Valid @RequestBody InterestRegisterRequest request) {
    Interest interest = interestService.create(request);
    InterestDto response = InterestMapper.toDto(interest,false);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "관심사 수정", description = "관심사의 키워드를 수정합니다.")
  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestDto> update(
      @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request
  ) {
    interestService.update(interestId, request);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리적으로 삭제합니다.")
  @DeleteMapping("/{interestId}")
  public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
    interestService.delete(interestId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "관심사 목록 조회", description = "조건에 맞는 관심사 목록을 조회합니다.")
  @GetMapping
  public CursorPageResponseDto<InterestDto> find(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "name") String orderBy,
      @RequestParam(defaultValue = "ASC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "10") int size,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    InterestSearchRequest request = new InterestSearchRequest(
        keyword,
        orderBy,
        direction,
        cursor,
        after,
        size,
        userId.toString()
    );

    return interestService.find(request);
  }

  @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
  @PostMapping("/{interestId}/subscribe")
  public void subscribe(
      @RequestParam UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    interestService.subscribe(userId, interestId);
  }

  // 구독 취소
  @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
  @DeleteMapping("/{interestId}/subscribe")
  public void unsubscribe(
      @RequestParam UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    interestService.unsubscribe(userId, interestId);
  }
}