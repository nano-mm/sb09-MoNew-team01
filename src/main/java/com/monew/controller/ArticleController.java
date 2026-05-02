package com.monew.controller;

import com.monew.dto.response.ArticleRestoreResultDto;
import com.monew.dto.response.ArticleViewDto;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.enums.ArticleSource;
import com.monew.service.ArticleBackupService;
import com.monew.service.ArticleService;
import com.monew.service.ArticleViewService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleViewService articleViewService;
  private final ArticleBackupService articleBackupService;

  @GetMapping("/sources")
  public ResponseEntity<List<String>> getSources() {
    log.info("[뉴스 기사] 출처 조회 요청 수신");
    List<String> sources = articleService.getSources();
    log.debug("[뉴스 기사] 출처 조회 요청 처리 완료");
    return ResponseEntity.ok(sources);
  }

  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<ArticleViewDto> createArticleView(@PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    log.info("[뉴스 기사] 기사 뷰 등록 요청 수신: userId={}", userId);
    ArticleViewDto responseDto = articleViewService.create(articleId, userId);

    return ResponseEntity.ok(responseDto);
  }
  @GetMapping
  public ResponseEntity<CursorPageResponseDto<ArticleDto>> searchArticles(
      @ModelAttribute ArticleSearchCondition searchRequest,
      @RequestParam(name = "sourceIn", required = false) List<ArticleSource> sourceIn,
      @Valid @ModelAttribute CursorRequest cursorRequest,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ){
    log.info("[뉴스 기사] 조회 요청 수신: userId={}", userId);
    CursorPageResponseDto<ArticleDto> responseDto = articleService.findArticles(searchRequest, sourceIn, cursorRequest, userId);
    log.debug("[뉴스 기사] 조회 요청 처리 완료: userId={}", userId);
    return ResponseEntity.ok(responseDto);
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleDto> search(@Valid @PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ){
    log.info("[뉴스 기사] 단건 조회 요청 수신: articleId={}", articleId);
    ArticleDto result = articleService.find(articleId);
    log.debug("[뉴스 기사] 단건 조회 요청 처리 완료: articleId={}", articleId);
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> softDelete(@Valid @PathVariable UUID articleId){
    log.info("[뉴스 기사] 논리 삭제 요청 수신: articleId={}", articleId);
    articleService.softDelete(articleId);
    log.debug("[뉴스 기사] 논리 삭제 요청 처리 완료: articleId={}", articleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDelete(@Valid @PathVariable UUID articleId){
    log.info("[뉴스 기사] 물리 삭제 요청 수신: articleId={}", articleId);
    articleService.hardDelete(articleId);
    log.debug("[뉴스 기사] 물리 삭제 요청 처리 완료: articleId={}", articleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/restore")
  public ResponseEntity<ArticleRestoreResultDto> restoreArticles(
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

      @Parameter(description = "날짜 끝(범위)")
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to)
      throws IOException {

    log.info("[뉴스 기사] 복구 요청 수신: from={}, to={}", from, to);
    ArticleRestoreResultDto responseDto = articleBackupService.importBackup(from, to);
    log.debug("[뉴스 기사] 복구 요청 처리 완료: from={}, to={}", from, to);

    return ResponseEntity.ok(responseDto);
  }

  @PostMapping("/collect")
  public ResponseEntity<Void> collectArticlesManually(
      @RequestHeader("Monew-Request-User-ID") UUID userId) {

    log.info("[뉴스 기사] 수동 수집 요청 수신");
    articleService.collect();

    return ResponseEntity.ok().build();
  }
}
