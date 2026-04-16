package com.monew.controller;

import com.monew.dto.response.ArticleViewDto;
import com.monew.service.ArticleService;
import com.monew.service.ArticleViewService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/api/articles")
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleViewService articleViewService;

  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<ArticleViewDto> createArticleView(@PathVariable UUID articleId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {

    ArticleViewDto responseDto = articleViewService.create(articleId, userId);

    return ResponseEntity.ok(responseDto);
  }
}
