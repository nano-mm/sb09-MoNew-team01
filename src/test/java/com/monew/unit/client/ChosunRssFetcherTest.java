package com.monew.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import org.mockito.Mockito;

import com.monew.adapter.out.news.ArticleFetchers.ChosunRssFetcher;
import com.monew.domain.model.enums.ArticleSource;
import com.rometools.rome.feed.synd.SyndContent;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChosunRssFetcherTest {

  private ChosunRssFetcher chosunRssFetcher;

  @Mock
  private SyndEntry mockEntry;

  @Mock
  private SyndContent mockDescription;

  @BeforeEach
  void setUp() {
    chosunRssFetcher = new ChosunRssFetcher();
  }

  @Test
  @DisplayName("메타데이터 검증 - URL 및 출처 이름이 정확해야 한다")
  void metadata_Success() {
    String url = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "getRssUrl");
    assertThat(url).isEqualTo("https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml");
    assertThat(chosunRssFetcher.getSourceName()).isEqualTo(ArticleSource.CHOSUN.toString());
  }

  @Test
  @DisplayName("XML 전처리 - 제어 문자(Control Characters)가 깔끔하게 제거된다")
  void preprocessXml_RemovesControlCharacters() {
    String rawXml = "<title>테스트\u0000기사\u0008입니다</title>";

    String cleanedXml = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "preprocessXml", rawXml);

    assertThat(cleanedXml).isEqualTo("<title>테스트기사입니다</title>");
  }

  @Test
  @DisplayName("요약 추출 - RSS 기본 Description이 존재하면 Jsoup 크롤링을 생략한다")
  void getSummary_HasDescription_ReturnsDescription() {
    given(mockEntry.getDescription()).willReturn(mockDescription);
    given(mockDescription.getValue()).willReturn("RSS에서 제공하는 기본 요약문입니다.");

    String summary = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "getSummary", mockEntry);

    assertThat(summary).isEqualTo("RSS에서 제공하는 기본 요약문입니다.");
  }

  @Test
  @DisplayName("요약 추출 - Description이 비어있으면 Jsoup으로 본문을 긁어온다 (100자 이하)")
  void getSummary_EmptyDescription_ScrapesFusionData() throws IOException {
    given(mockEntry.getDescription()).willReturn(mockDescription);
    given(mockDescription.getValue()).willReturn(""); // 빈 요약문 세팅
    given(mockEntry.getLink()).willReturn("https://chosun.com/test-article");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      Connection connectionMock = mock(Connection.class);
      Document documentMock = mock(Document.class);
      Element scriptMock = mock(Element.class);

      jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(connectionMock);
      given(connectionMock.timeout(anyInt())).willReturn(connectionMock);
      given(connectionMock.userAgent(anyString())).willReturn(connectionMock);
      given(connectionMock.get()).willReturn(documentMock);

      String fakeFusionScript = "window.Fusion; \"content\":\"크롤링으로 추출해낸 본문입니다.\",\"type\":\"text\"";
      given(scriptMock.html()).willReturn(fakeFusionScript);
      given(documentMock.select("script")).willReturn(new Elements(scriptMock));

      String summary = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "getSummary", mockEntry);

      assertThat(summary).isEqualTo("크롤링으로 추출해낸 본문입니다.");
    }
  }

  @Test
  @DisplayName("요약 추출 - 크롤링한 본문이 100자를 초과하면 잘라내고 '...'을 붙인다")
  void getSummary_ScrapedDataOver100Chars_Truncates() throws IOException {
    given(mockEntry.getDescription()).willReturn(mockDescription);
    given(mockDescription.getValue()).willReturn("");
    given(mockEntry.getLink()).willReturn("https://chosun.com/test-article");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      Connection connectionMock = mock(Connection.class);
      Document documentMock = mock(Document.class);
      Element scriptMock = mock(Element.class);

      jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(connectionMock);
      given(connectionMock.timeout(anyInt())).willReturn(connectionMock);
      given(connectionMock.userAgent(anyString())).willReturn(connectionMock);
      given(connectionMock.get()).willReturn(documentMock);

      String longText = "가".repeat(110);
      String fakeFusionScript = "window.Fusion; \"content\":\"" + longText + "\",\"type\":\"text\"";
      given(scriptMock.html()).willReturn(fakeFusionScript);
      given(documentMock.select("script")).willReturn(new Elements(scriptMock));

      String summary = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "getSummary", mockEntry);

      assertThat(summary).hasSize(103); // 100글자 + "..." (3글자)
      assertThat(summary).endsWith("...");
    }
  }

  @Test
  @DisplayName("XML 전처리 - null이나 빈 문자열이 들어오면 빈 문자열을 반환한다")
  void preprocessXml_NullOrBlank_ReturnsEmptyString() {
    String nullResult = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "preprocessXml", (String) null);
    assertThat(nullResult).isEqualTo("");

    String blankResult = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "preprocessXml", "   ");
    assertThat(blankResult).isEqualTo("");
  }

  @Test
  @DisplayName("요약 추출 - Jsoup 크롤링 중 예외(Timeout 등) 발생 시 기본 안내문을 반환한다")
  void getSummary_JsoupException_ReturnsFallbackMessage() {
    given(mockEntry.getDescription()).willReturn(mockDescription);
    given(mockDescription.getValue()).willReturn("");
    given(mockEntry.getLink()).willReturn("https://chosun.com/test-article");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class, Mockito.CALLS_REAL_METHODS)) {
      jsoupMock.when(() -> Jsoup.connect(anyString())).thenThrow(new RuntimeException("Connection Timeout"));

      String summary = ReflectionTestUtils.invokeMethod(chosunRssFetcher, "getSummary", mockEntry);

      assertThat(summary).isEqualTo("요약 정보를 불러올 수 없습니다.");
    }
  }
}