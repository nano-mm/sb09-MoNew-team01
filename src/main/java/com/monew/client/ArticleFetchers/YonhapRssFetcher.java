package com.monew.client.ArticleFetchers;

import com.monew.entity.enums.ArticleSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YonhapRssFetcher extends BaseRssFetcher {

  @Override
  protected String getRssUrl() {
    return "https://www.yna.co.kr/rss/news.xml";
  }

  @Override
  protected String preprocessXml(String rawXml) {
    if (rawXml == null || rawXml.isBlank()) {
      return "";
    }

    return rawXml.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");
  }

  @Override
  public String getSourceName() {
    return ArticleSource.YONHAP.toString();
  }
}
