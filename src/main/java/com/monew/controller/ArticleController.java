package com.monew.controller;

import com.monew.dto.response.ArticleViewDto;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.service.ArticleService;
import com.monew.service.ArticleViewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleViewService articleViewService;

  @GetMapping("/sources")
  public ResponseEntity<List<String>> getSources() {

    List<String> sources = articleService.getSources();

    return ResponseEntity.ok(sources);
  }

  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<ArticleViewDto> createArticleView(@PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {

    ArticleViewDto responseDto = articleViewService.create(articleId, userId);

    return ResponseEntity.ok(responseDto);
  }
  @GetMapping
  public ResponseEntity<CursorPageResponseDto<ArticleDto>> searchArticles(
      @ModelAttribute ArticleSearchCondition searchRequest,
      @Valid @ModelAttribute CursorRequest cursorRequest,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ){
      CursorPageResponseDto<ArticleDto> responseDto = articleService.findArticles(searchRequest, cursorRequest, userId);

      return ResponseEntity.ok(responseDto);
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleDto> search(@Valid @PathVariable UUID articleId){
    ArticleDto result = articleService.find(articleId);

    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> softDelete(@Valid @PathVariable UUID articleId){
    articleService.softDelete(articleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDelete(@Valid @PathVariable UUID articleId){
    articleService.hardDelete(articleId);
    return ResponseEntity.noContent().build();
  }
}
