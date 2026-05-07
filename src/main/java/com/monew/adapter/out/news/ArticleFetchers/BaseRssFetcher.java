package com.monew.application.port.out.news.ArticleFetchers;

import com.monew.application.port.out.news.ArticleFetcher;
import com.monew.dto.response.ArticleDto;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@Slf4j
public abstract class BaseRssFetcher implements ArticleFetcher {
  @Autowired
  private RestTemplate restTemplate;

  protected abstract String getRssUrl();

  @Override
  public List<ArticleDto> fetch(Set<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return new ArrayList<>();
    }

    List<ArticleDto> result = new ArrayList<>();
    try {
      String rawXmlContent = restTemplate.getForObject(getRssUrl(), String.class);
      if (rawXmlContent == null || rawXmlContent.isBlank()) return result;

      String processedXml = preprocessXml(rawXmlContent);

      SyndFeedInput input = new SyndFeedInput();
      input.setAllowDoctypes(true);
      SyndFeed feed = input.build(new StringReader(processedXml));

      for (SyndEntry entry : feed.getEntries()) {
        if (isMatched(entry, keywords)) {
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
        .summary(summary)
        .publishDate(parseDate(entry.getPublishedDate()))
        .build();
  }

  protected String getSummary(SyndEntry entry){
    if (entry.getDescription() == null || entry.getDescription().getValue() == null) {
      return "요약이 제공되지 않는 기사입니다.";
    }

    String description = cleanHtml(entry.getDescription().getValue());
    return !description.isEmpty() ? description : "요약이 제공되지 않는 기사입니다.";
  }

  protected String cleanHtml(String text) {
    if (text == null) return "";
    return Jsoup.parse(text).text().trim();
  }

  private boolean isMatched(SyndEntry entry, Set<String> keywords) {
    String title = entry.getTitle() != null ? entry.getTitle() : "";
    String description = entry.getDescription() != null
        ? cleanHtml(entry.getDescription().getValue())
        : "";

    String target = (title + " " + description).toLowerCase();

    return keywords.stream().anyMatch(keyword -> {
      String lowerKeyword = keyword.toLowerCase();

      if (lowerKeyword.matches("^[a-z]+$") && lowerKeyword.length() <= 3) {
        return target.matches(".*\\b" + lowerKeyword + "\\b.*");
      }

      return target.contains(lowerKeyword);
    });
  }

  private Instant parseDate(Date date) {
    return (date != null) ? date.toInstant() : Instant.now();
  }

}
