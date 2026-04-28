package com.monew.client.ArticleFetchers;

import com.monew.client.ArticleFetcher;
import com.monew.dto.response.ArticleDto;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

@Slf4j
public abstract class BaseRssFetcher implements ArticleFetcher {
    protected abstract String getRssUrl();

  @Override
  public List<ArticleDto> fetch(String keyword) {
    List<ArticleDto> result = new ArrayList<>();
    try {
      RestTemplate restTemplate = new RestTemplate();
      String rawXmlContent = restTemplate.getForObject(getRssUrl(), String.class);
      if (rawXmlContent == null || rawXmlContent.isBlank()) return result;

      String processedXml = preprocessXml(rawXmlContent);

      SyndFeedInput input = new SyndFeedInput();
      input.setAllowDoctypes(true);
      SyndFeed feed = input.build(new StringReader(processedXml));

      for (SyndEntry entry : feed.getEntries()) {
        if (isMatched(entry, keyword)) {
          result.add(convertToDto(entry));
        }
      }
    } catch (Exception e) {
      log.error("[{}] 수집 실패 사유: {}", getSourceName(), e.getMessage());
    }
    return result;
  }

  protected String preprocessXml(String xml) {
    return xml;
  }

  protected ArticleDto convertToDto(SyndEntry entry) {
    String sourceUrl = entry.getLink();

    String summary = this.getSummary(entry);

    return ArticleDto.builder()
        .source(getSourceName())
        .sourceUrl(sourceUrl)
        .title(cleanHtml(entry.getTitle()))
        .summary(cleanHtml(summary))
        .publishDate(parseDate(entry.getPublishedDate()))
        .build();
  }

  protected String getSummary(SyndEntry entry){
    return entry.getDescription() != null ? entry.getDescription().getValue() : "요약이 제공되지 않는 기사입니다.";
  }

  private boolean isMatched(SyndEntry entry, String keyword) {
    String title = entry.getTitle() != null ? entry.getTitle() : "";
    String description = entry.getDescription() != null ? entry.getDescription().getValue() : "";
    String target = (title + description).toLowerCase();
    return target.contains(keyword.toLowerCase());
  }

  private String cleanHtml(String text) {
    if (text == null) return "";
    return text.replaceAll("<[^>]*>", "").trim();
  }

  private Instant parseDate(Date date) {
    return (date != null) ? date.toInstant() : Instant.now();
  }
}
