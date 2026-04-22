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
  protected String preprocessXml(String rawXml) {
    if (rawXml == null || rawXml.isBlank()) {
      return "";
    }

    String cleanedXml = rawXml.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

    return cleanedXml;
  }

  @Override
  public String getSourceName() {
    return ArticleSource.CHOSUN.toString();
  }
}
