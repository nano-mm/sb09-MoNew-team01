package com.monew.client;

import com.monew.dto.response.ArticleDto;
import java.util.List;

public interface ArticleFetcher {

  List<ArticleDto> fetch(String keyword);

  String getSourceName();
}
