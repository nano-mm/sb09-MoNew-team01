package com.monew.client.ArticleFetchers;

import com.monew.entity.enums.ArticleSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChosunRssFetcher extends BaseRssFetcher {

  @Override
  protected String getRssUrl() {
    return "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";
  }

  @Override
  public String getSourceName() {
    return ArticleSource.CHOSUN.toString();
  }
}
