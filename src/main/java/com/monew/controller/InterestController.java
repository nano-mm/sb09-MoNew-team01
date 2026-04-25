package com.monew.controller;

import com.monew.config.LoginUser;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.dto.response.SubscriptionDto;
import com.monew.entity.Interest;
import com.monew.mapper.InterestMapper;
import com.monew.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

  // 요청 파라미터와 헤더가 안맞아서 수정했습니다.
  @Operation(summary = "관심사 목록 조회", description = "조건에 맞는 관심사 목록을 조회합니다.")
  @GetMapping
  public CursorPageResponseDto<InterestDto> find(
      @RequestParam(required = false) String keyword,
      @Valid @ModelAttribute CursorRequest cursorRequest,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {

    // 로그 추가해야합니다

    CursorPageResponseDto<InterestDto> result = interestService.find(keyword, cursorRequest, userId);

    return result;
  }

  // 응답으로 SubscriptionDto을 반환해야 하는데 아무것도 반환하지 않아서 이부분 수정했습니다
  @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
  @PostMapping("/{interestId}/subscriptions")
  public ResponseEntity<SubscriptionDto> subscribe(
      @PathVariable UUID interestId,
      @LoginUser UUID userId
  ) {
    SubscriptionDto result = interestService.subscribe(userId, interestId);

    return ResponseEntity.ok(result);
  }


  // 구독 취소 후 결과 반환 추가 했습니다

  // 구독 취소
  @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
  @DeleteMapping("/{interestId}/subscriptions")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @LoginUser UUID userId
  ) {
    interestService.unsubscribe(userId, interestId);
    return ResponseEntity.noContent().build();
  }

}