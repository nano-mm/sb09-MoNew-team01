package com.monew.service;

import com.monew.dto.response.ArticleDto;
import java.util.UUID;

public interface ArticleService {

  void collect();

  ArticleDto find(UUID articleId);

  void delete(UUID articleId);

}
