package com.monew.client.ArticleFetchers;

import com.monew.entity.enums.ArticleSource;
import com.rometools.rome.feed.synd.SyndEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Slf4j
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

  @Override
  protected String getSummary(SyndEntry entry){
    String description = cleanHtml(entry.getDescription().getValue());
    return !description.isEmpty() ? description :fetchSummaryFromUrl(entry.getLink());
  }

  private String fetchSummaryFromUrl(String articleUrl) {
    try {
      Document doc = Jsoup.connect(articleUrl)
          .timeout(1000)
          .userAgent("Mozilla/5.0")
          .get();

      String fusionData = "";
      for (Element script : doc.select("script")) {
        if (script.html().contains("window.Fusion")) {
          fusionData = script.html();
          break;
        }
      }

      Pattern pattern = Pattern.compile("\"content\":\"([^\"]+)\",\"type\":\"text\"");
      Matcher matcher = pattern.matcher(fusionData);

      String content = "";
      if (matcher.find()) {
        content = matcher.group(1);
      }

      if (content.isEmpty()) {
        return "요약이 제공되지 않는 기사입니다.";
      }

      content = content.trim();
      int maxLength = 100;

      if (content.length() > maxLength) {
        return content.substring(0, maxLength) + "...";
      }

      return content;

    } catch (Exception e) {
      log.warn("[뉴스 기사 수집 - 요약 추출 실패] URL: {}, 사유: {}", articleUrl, e.getMessage());
      return "요약 정보를 불러올 수 없습니다.";
    }
  }
}
