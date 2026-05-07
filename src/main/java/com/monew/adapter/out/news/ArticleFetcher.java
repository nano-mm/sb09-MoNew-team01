package com.monew.adapter.out.news;

import com.monew.dto.response.ArticleDto;
import java.util.List;
import java.util.Set;

public interface ArticleFetcher {

  List<ArticleDto> fetch(Set<String> keywords);

  String getSourceName();
}
