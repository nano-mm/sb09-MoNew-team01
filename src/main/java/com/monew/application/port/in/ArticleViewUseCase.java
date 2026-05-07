package com.monew.application.port.in;

import com.monew.dto.response.ArticleViewDto;
import java.util.UUID;

public interface ArticleViewUseCase {
  ArticleViewDto create(UUID articleId, UUID userId);
}
