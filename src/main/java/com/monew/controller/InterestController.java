package com.monew.controller;

import com.monew.config.LoginUser;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.dto.response.SubscriptionDto;
import com.monew.entity.Interest;
import com.monew.mapper.InterestMapper;
import com.monew.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "관심사 관리", description = "관심사 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController {

  private final InterestService interestService;

  @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
  @PostMapping
  public ResponseEntity<InterestDto> create(
      @Valid @RequestBody InterestRegisterRequest request
  ) {
    log.info("[관심사] 등록 요청 수신: name={}", request.name());

    Interest interest = interestService.create(request);
    InterestDto response = InterestMapper.toDto(interest,false);

    log.debug("[관심사] 등록 완료: interestId={}", interest.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "관심사 수정", description = "관심사의 키워드를 수정합니다.")
  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestDto> update(
      @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request
  ) {
    log.info("[관심사] 수정 요청 수신: interestId={}", interestId);
    interestService.update(interestId, request);

    log.debug("[관심사] 수정 완료: interestId={}", interestId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리적으로 삭제합니다.")
  @DeleteMapping("/{interestId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID interestId
  ) {
    log.info("[관심사] 삭제 요청 수신: interestId={}", interestId);
    interestService.delete(interestId);

    log.debug("[관심사] 삭제 완료: interestId={}", interestId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "관심사 목록 조회", description = "조건에 맞는 관심사 목록을 조회합니다.")
  @GetMapping
  public CursorPageResponseDto<InterestDto> find(
      @RequestParam(required = false) String keyword,
      @Valid @ModelAttribute CursorRequest cursorRequest,
      @LoginUser UUID userId
  ) {
    log.info("[관심사] 목록 조회 요청 수신: userId={}, keyword={}, orderBy={}, direction={}",
        userId, keyword, cursorRequest.orderBy(), cursorRequest.direction());

    InterestSearchRequest request = new InterestSearchRequest(
        keyword,
        cursorRequest,
        userId
    );

    CursorPageResponseDto<InterestDto> response = interestService.find(keyword, cursorRequest, userId);

    log.debug("[관심사] 목록 조회 완료: userId={}, size={}, hasNext={}",
        userId, response.size(), response.hasNext());

    return response;
  }

  @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
  @PostMapping("/{interestId}/subscriptions")
  public ResponseEntity<SubscriptionDto> subscribe(
      @PathVariable UUID interestId,
      @LoginUser UUID userId
  ) {
    log.info("[관심사] 구독 요청 수신: userId={}, interestId={}", userId, interestId);
    SubscriptionDto subscriptionDto = interestService.subscribe(userId, interestId);

    log.debug("[관심사] 구독 완료: userId={}, interestId={}", userId, interestId);
    return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionDto);
  }

  // 구독 취소
  @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
  @DeleteMapping("/{interestId}/subscriptions")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @LoginUser UUID userId
  ) {
    log.info("[관심사] 구독 취소 요청 수신: userId={}, interestId={}", userId, interestId);
    interestService.unsubscribe(userId, interestId);

    log.debug("[관심사] 구독 취소 완료: userId={}, interestId={}", userId, interestId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}