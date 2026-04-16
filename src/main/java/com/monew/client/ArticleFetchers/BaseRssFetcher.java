package com.monew.client.ArticleFetchers;

import com.monew.client.ArticleFetcher;
import com.monew.dto.response.ArticleDto;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.StringReader;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
      String xmlContent = restTemplate.getForObject(getRssUrl(), String.class);
      if (xmlContent == null) return result;

      List<String> items = new ArrayList<>();
      Pattern itemPattern = Pattern.compile("<item[\\s\\S]*?<\\/item>");
      Matcher matcher = itemPattern.matcher(xmlContent);
      while (matcher.find()) {
        items.add(matcher.group());
      }

      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      sb.append("<rss version=\"2.0\" ");
      sb.append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ");
      sb.append("xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" ");
      sb.append("xmlns:media=\"http://search.yahoo.com/mrss/\" ");
      sb.append("xmlns:atom=\"http://www.w3.org/2005/Atom\" ");
      sb.append("xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">");
      sb.append("<channel><title>Unified RSS Channel</title>");

      for (String item : items) {
        sb.append(item.replaceAll("(?i)\\s(async|crossorigin|defer)\\b", ""));
      }
      sb.append("</channel></rss>");

      SyndFeedInput input = new SyndFeedInput();
      input.setAllowDoctypes(true);
      SyndFeed feed = input.build(new StringReader(sb.toString()));

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

  protected ArticleDto convertToDto(SyndEntry entry) {
    String sourceUrl = entry.getLink();

    String summary = entry.getDescription() != null ? entry.getDescription().getValue() : "";

    return ArticleDto.builder()
        .source(getSourceName())
        .sourceUrl(sourceUrl)
        .title(cleanHtml(entry.getTitle()))
        .summary(cleanHtml(summary))
        .publishDate(parseDate(entry.getPublishedDate()))
        .build();
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
