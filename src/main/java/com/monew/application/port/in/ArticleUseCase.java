package com.monew.application.port.in;

import com.monew.domain.model.enums.ArticleSource;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import java.util.List;
import java.util.UUID;

public interface ArticleUseCase {
  void collect();
  List<String> getSources();
  CursorPageResponseDto<ArticleDto> findArticles(ArticleSearchCondition condition, List<ArticleSource> sourceIn, CursorRequest cursorRequest, UUID userId);
  ArticleDto find(UUID articleId);
  void softDelete(UUID articleId);
  void hardDelete(UUID articleId);
}
