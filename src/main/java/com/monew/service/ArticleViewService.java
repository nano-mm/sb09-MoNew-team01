package com.monew.service;

import com.monew.dto.response.ArticleViewDto;
import java.util.UUID;

public interface ArticleViewService {

  ArticleViewDto create(UUID articleId, UUID requestUserId);
}
