package com.monew.service;

import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import java.util.UUID;

public interface ArticleService {

  void collect();

  CursorPageResponseDto<ArticleDto> findArticles(ArticleSearchCondition searchRequest
      , CursorRequest cursorRequest, UUID userId);

  ArticleDto find(UUID articleId);

  void delete(UUID articleId);

}
