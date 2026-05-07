package com.monew.application.port.out.news.ArticleFetchers;

import com.monew.application.port.out.news.ArticleFetcher;

import com.monew.dto.news.NaverNewsItem;
import com.monew.dto.news.NaverNewsResponse;
import com.monew.dto.response.ArticleDto;
import com.monew.domain.model.enums.ArticleSource;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverApiFetcher implements ArticleFetcher {

  private final RestTemplate restTemplate;

  private static final int CHUNK_SIZE = 5;

  @Value("${naver.client.id}")
  private String clientId;

  @Value("${naver.client.secret}")
  private String clientSecret;

  private static final DateTimeFormatter NAVER_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.KOREA);

  @Override
  public List<ArticleDto> fetch(Set<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return new ArrayList<>();
    }

    List<ArticleDto> articles = new ArrayList<>();

    List<String> keywordList = new ArrayList<>(keywords);

    for (int i = 0; i < keywordList.size(); i += CHUNK_SIZE) {
      int end = Math.min(keywordList.size(), i + CHUNK_SIZE);
      List<String> chunk = keywordList.subList(i, end);

      String combinedQuery = String.join("|", chunk);

      try {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://openapi.naver.com/v1/search/news.json")
            .queryParam("query", combinedQuery)
            .queryParam("display", 50) // 청크당 50개씩 수집
            .queryParam("sort", "date")
            .build()
            .encode()
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<NaverNewsResponse> response = restTemplate.exchange(
            uri, HttpMethod.GET, entity, NaverNewsResponse.class
        );

        NaverNewsResponse body = response.getBody();
        if (body != null && body.items() != null) {
          for (NaverNewsItem item : body.items()) {
            articles.add(convertToDto(item));
          }
        }

      } catch (Exception e) {
        log.error("네이버 API 뉴스 수집 중 오류 발생 - 쿼리: {}", combinedQuery, e);
      }

      try {
        // api 부하 줄이기 용
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    return articles ;
  }

  private ArticleDto convertToDto(NaverNewsItem item) {
    return ArticleDto.builder()
        .source(getSourceName())
        .sourceUrl(item.originallink() != null && !item.originallink().isEmpty()
            ? item.originallink() : item.link())
        .title(cleanHtml(item.title()))
        .summary(cleanHtml(item.description()))
        .publishDate(parseDate(item.pubDate()))
        .build();
  }

  // HTML 태그(<...>) 제거 및 HTML 엔티티(&quot;, &amp; 등) 복원
  private String cleanHtml(String text) {
    if (text == null) return "";
    String withoutTags = text.replaceAll("<[^>]*>", "");
    return HtmlUtils.htmlUnescape(withoutTags);
  }

  private Instant parseDate(String pubDate) {
    if (pubDate == null || pubDate.isBlank()) {
      return Instant.now();
    }

    try {
      String cleanDate = pubDate.trim();

      ZonedDateTime zdt = ZonedDateTime.parse(cleanDate, DateTimeFormatter.RFC_1123_DATE_TIME);
      return zdt.toInstant();
    } catch (Exception e) {
      log.warn("날짜 파싱 실패: [{}] - 사유: {}", pubDate, e.getMessage());
      return Instant.now();
    }
  }

  @Override
  public String getSourceName() {
    return ArticleSource.NAVER.toString();
  }
}