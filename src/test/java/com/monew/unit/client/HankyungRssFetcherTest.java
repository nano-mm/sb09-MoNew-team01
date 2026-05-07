package com.monew.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.monew.adapter.out.news.ArticleFetchers.HankyungRssFetcher;
import com.monew.domain.model.enums.ArticleSource;
import com.rometools.rome.feed.synd.SyndEntry;
import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HankyungRssFetcherTest {

  private HankyungRssFetcher hankyungRssFetcher;

  @Mock
  private SyndEntry mockEntry;

  @BeforeEach
  void setUp() {
    hankyungRssFetcher = new HankyungRssFetcher();
  }

  @Test
  @DisplayName("메타데이터 검증 - URL 및 출처 이름이 정확해야 한다")
  void metadata_Success() {
    String url = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "getRssUrl");
    assertThat(url).isEqualTo("https://www.hankyung.com/feed/all-news");
    assertThat(hankyungRssFetcher.getSourceName()).isEqualTo(ArticleSource.HANKYUNG.toString());
  }

  @Test
  @DisplayName("XML 전처리 - 제어 문자가 깔끔하게 제거된다")
  void preprocessXml_RemovesControlCharacters() {
    String rawXml = "<title>한국\u0000경제\u0008뉴스</title>";
    String cleanedXml = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "preprocessXml", rawXml);
    assertThat(cleanedXml).isEqualTo("<title>한국경제뉴스</title>");
  }

  @Test
  @DisplayName("요약 추출 - .article-body가 없을 경우 기본 안내문을 반환한다")
  void getSummary_NoArticleBody_ReturnsFallback() throws IOException {
    given(mockEntry.getLink()).willReturn("https://hankyung.com/test");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      Connection connectionMock = mock(Connection.class);
      Document documentMock = mock(Document.class);

      jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(connectionMock);
      given(connectionMock.timeout(anyInt())).willReturn(connectionMock);
      given(connectionMock.userAgent(anyString())).willReturn(connectionMock);
      given(connectionMock.get()).willReturn(documentMock);

      given(documentMock.selectFirst(".article-body")).willReturn(null);

      String summary = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "getSummary", mockEntry);

      assertThat(summary).isEqualTo("요약이 제공되지 않는 기사입니다.");
    }
  }

  @Test
  @DisplayName("요약 추출 - 이미지(.article-figure)를 제거하고 100자 이하의 본문을 반환한다")
  void getSummary_RemovesFigure_ReturnsShortText() {
    given(mockEntry.getLink()).willReturn("https://hankyung.com/test");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      Connection connectionMock = mock(Connection.class);
      Document documentMock = mock(Document.class);
      Element articleBodyMock = mock(Element.class);
      Elements figureElementsMock = mock(Elements.class);

      jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(connectionMock);
      given(connectionMock.timeout(anyInt())).willReturn(connectionMock);
      given(connectionMock.userAgent(anyString())).willReturn(connectionMock);
      given(connectionMock.get()).willReturn(documentMock);

      given(documentMock.selectFirst(".article-body")).willReturn(articleBodyMock);
      given(articleBodyMock.select(".article-figure")).willReturn(figureElementsMock);

      given(articleBodyMock.text()).willReturn("한국경제 본문입니다.");

      String summary = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "getSummary", mockEntry);

      assertThat(summary).isEqualTo("한국경제 본문입니다.");

      verify(figureElementsMock).remove();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("요약 추출 - 크롤링한 본문이 100자를 초과하면 잘라내고 '...'을 붙인다")
  void getSummary_TextOver100Chars_Truncates() throws IOException {
    given(mockEntry.getLink()).willReturn("https://hankyung.com/test");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      Connection connectionMock = mock(Connection.class);
      Document documentMock = mock(Document.class);
      Element articleBodyMock = mock(Element.class);
      Elements figureElementsMock = mock(Elements.class);

      jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(connectionMock);
      given(connectionMock.timeout(anyInt())).willReturn(connectionMock);
      given(connectionMock.userAgent(anyString())).willReturn(connectionMock);
      given(connectionMock.get()).willReturn(documentMock);

      given(documentMock.selectFirst(".article-body")).willReturn(articleBodyMock);
      given(articleBodyMock.select(".article-figure")).willReturn(figureElementsMock);

      String longText = "가".repeat(110);
      given(articleBodyMock.text()).willReturn(longText);

      String summary = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "getSummary", mockEntry);

      assertThat(summary).hasSize(103);
      assertThat(summary).endsWith("...");
    }
  }

  @Test
  @DisplayName("XML 전처리 - null이나 빈 문자열이 들어오면 빈 문자열을 반환한다")
  void preprocessXml_NullOrBlank_ReturnsEmptyString() {
    String nullResult = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "preprocessXml", (String) null);
    assertThat(nullResult).isEqualTo("");

    String blankResult = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "preprocessXml", "   ");
    assertThat(blankResult).isEqualTo("");
  }

  @Test
  @DisplayName("요약 추출 - Jsoup 크롤링 중 예외 발생 시 안내문을 반환한다")
  void getSummary_JsoupException_ReturnsFallbackMessage() {
    given(mockEntry.getLink()).willReturn("https://hankyung.com/test");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      jsoupMock.when(() -> Jsoup.connect(anyString())).thenThrow(new RuntimeException("Connection Refused"));

      String summary = ReflectionTestUtils.invokeMethod(hankyungRssFetcher, "getSummary", mockEntry);

      assertThat(summary).isEqualTo("요약 정보를 불러올 수 없습니다.");
    }
  }
}
