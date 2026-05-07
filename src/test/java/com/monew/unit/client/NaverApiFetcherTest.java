package com.monew.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import com.monew.application.port.out.news.ArticleFetchers.NaverApiFetcher;
import com.monew.dto.news.NaverNewsItem;
import com.monew.dto.news.NaverNewsResponse;
import com.monew.dto.response.ArticleDto;
import com.monew.domain.model.enums.ArticleSource;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class NaverApiFetcherTest {

  @InjectMocks
  private NaverApiFetcher naverApiFetcher;

  @Mock
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(naverApiFetcher, "clientId", "test-client-id");
    ReflectionTestUtils.setField(naverApiFetcher, "clientSecret", "test-client-secret");
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - 성공 및 HTML/날짜 파싱 검증")
  void fetch_Success() {
    // given
    String mockTitle = "<b>인공지능</b> &quot;혁신&quot;";
    String mockDescription = "AI 기술이 발전합니다.";
    String mockOriginalLink = "https://original.news.com/123";
    String mockLink = "https://n.news.naver.com/123";
    String mockPubDate = "Wed, 29 Apr 2026 16:33:17 +0900";

    NaverNewsItem mockItem = new NaverNewsItem(
        mockTitle, mockOriginalLink, mockLink, mockDescription, mockPubDate
    );

    NaverNewsResponse mockResponseBody = new NaverNewsResponse(
        "Wed, 29 Apr 2026 16:33:17 +0900",
        1,
        1,
        1,
        List.of(mockItem)
    );

    given(restTemplate.exchange(
        any(URI.class),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(NaverNewsResponse.class)
    )).willReturn(ResponseEntity.ok(mockResponseBody));

    List<ArticleDto> results = naverApiFetcher.fetch(Collections.singleton("인공지능"));

    assertThat(results).hasSize(1);

    ArticleDto article = results.get(0);
    assertThat(article.source()).isEqualTo(ArticleSource.NAVER.toString());
    assertThat(article.sourceUrl()).isEqualTo(mockOriginalLink);
    assertThat(article.title()).isEqualTo("인공지능 \"혁신\"");
    assertThat(article.summary()).isEqualTo("AI 기술이 발전합니다.");
    assertThat(article.publishDate()).isNotNull();
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - originallink가 없을 경우 link 사용")
  void fetch_FallbackToLink() {
    NaverNewsItem mockItem = new NaverNewsItem(
        "테스트", null, "https://n.news.naver.com/123", "테스트", "Wed, 29 Apr 2026 16:33:17 +0900"
    );

    NaverNewsResponse mockResponseBody = new NaverNewsResponse(
        "Wed, 29 Apr 2026 16:33:17 +0900", 1, 1, 1, List.of(mockItem)
    );

    given(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(NaverNewsResponse.class)))
        .willReturn(ResponseEntity.ok(mockResponseBody));

    List<ArticleDto> results = naverApiFetcher.fetch(Collections.singleton("인공지능"));

    assertThat(results.get(0).sourceUrl()).isEqualTo("https://n.news.naver.com/123");
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - 외부 API 예외 발생 시 빈 리스트 반환")
  void fetch_ApiError_ShouldReturnEmptyList() {
    given(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(NaverNewsResponse.class)))
        .willThrow(new RestClientException("Connection Timeout"));

    List<ArticleDto> results = naverApiFetcher.fetch(Collections.singleton("인공지능"));

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - 응답 body 또는 items가 null인 경우 빈 리스트 반환")
  void fetch_NullBodyOrItems_ReturnsEmptyList() {
    // 1. body가 null인 경우
    given(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(NaverNewsResponse.class)))
        .willReturn(ResponseEntity.ok(null));
    assertThat(naverApiFetcher.fetch(Collections.singleton("인공지능"))).isEmpty();

    // 2. items가 null인 경우
    NaverNewsResponse nullItemsResponse = new NaverNewsResponse(null, 0, 0, 0, null);
    given(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(NaverNewsResponse.class)))
        .willReturn(ResponseEntity.ok(nullItemsResponse));
    assertThat(naverApiFetcher.fetch(Collections.singleton("인공지능"))).isEmpty();
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - pubDate가 null이거나 파싱 불가능한 경우 현재 시간 반환")
  void fetch_InvalidPubDate_ReturnsNow() {
    NaverNewsItem mockItem1 = new NaverNewsItem("제목", "링크", "링크", "요약", "");
    NaverNewsItem mockItem2 = new NaverNewsItem("제목", "링크", "링크", "요약", "이상한 날짜 형식");

    NaverNewsResponse response = new NaverNewsResponse(null, 2, 1, 2, List.of(mockItem1, mockItem2));
    given(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(NaverNewsResponse.class)))
        .willReturn(ResponseEntity.ok(response));

    List<ArticleDto> results = naverApiFetcher.fetch(Collections.singleton("인공지능"));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).publishDate()).isNotNull();
    assertThat(results.get(1).publishDate()).isNotNull();
  }

  @Test
  @DisplayName("네이버 뉴스 검색 - title이나 description이 null일 때 cleanHtml 방어 로직 작동")
  void fetch_NullTitleOrDescription_HandledSafely() {
    NaverNewsItem nullTextItem = new NaverNewsItem(null, "링크", "링크", null, "Wed, 29 Apr 2026 16:33:17 +0900");
    NaverNewsResponse response = new NaverNewsResponse(null, 1, 1, 1, List.of(nullTextItem));

    given(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(NaverNewsResponse.class)))
        .willReturn(ResponseEntity.ok(response));

    List<ArticleDto> results = naverApiFetcher.fetch(Collections.singleton("인공지능"));

    assertThat(results.get(0).title()).isEmpty();
    assertThat(results.get(0).summary()).isEmpty();
  }

  @Test
  @DisplayName("API 수집 중 Thread.sleep 대기 시 InterruptedException이 발생하면 인터럽트 상태를 복구한다")
  void fetch_InterruptedException_RestoresInterruptFlag() throws InterruptedException {
    Set<String> keywords = Set.of("test");

    when(restTemplate.exchange(
        any(URI.class),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(NaverNewsResponse.class)
    )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

    AtomicBoolean isInterruptedFlagRestored = new AtomicBoolean(false);

    Thread targetThread = new Thread(() -> {

      Thread.currentThread().interrupt();

      naverApiFetcher.fetch(keywords);

      isInterruptedFlagRestored.set(Thread.currentThread().isInterrupted());
    });

    targetThread.start();
    targetThread.join();
    
    assertTrue(isInterruptedFlagRestored.get(),
        "InterruptedException catch 블록에서 인터럽트 상태(flag)가 복구되어야 합니다.");
  }

  @Test
  @DisplayName("출처 이름 반환 확인")
  void getSourceName_Success() {
    assertThat(naverApiFetcher.getSourceName()).isEqualTo("NAVER");
  }
}
