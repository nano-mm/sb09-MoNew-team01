package com.monew.client.ArticleFetchers;

import com.monew.entity.enums.ArticleSource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HankyungRssFetcher extends BaseRssFetcher {

  @Override
  protected String getRssUrl() {
    return "https://www.hankyung.com/feed/all-news";
  }

  @Override
  public String getSourceName() {
    return ArticleSource.HANKYUNG.toString();
  }
}
