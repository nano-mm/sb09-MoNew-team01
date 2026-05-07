package com.monew.unit.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.adapter.out.news.ArticleFetchers.YonhapRssFetcher;
import com.monew.domain.model.enums.ArticleSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class YonhapRssFetcherTest {

  private YonhapRssFetcher yonhapRssFetcher;

  @BeforeEach
  void setUp() {
    yonhapRssFetcher = new YonhapRssFetcher();
  }

  @Test
  @DisplayName("메타데이터 검증 - 연합뉴스 RSS URL 및 출처 이름이 정확해야 한다")
  void metadata_Success() {
    String url = ReflectionTestUtils.invokeMethod(yonhapRssFetcher, "getRssUrl");
    String sourceName = yonhapRssFetcher.getSourceName();

    assertThat(url).isEqualTo("https://www.yna.co.kr/rss/news.xml");
    assertThat(sourceName).isEqualTo(ArticleSource.YONHAP.toString());
  }

  @Test
  @DisplayName("XML 전처리 - 정상적인 문자열은 그대로 반환된다")
  void preprocessXml_NormalString_ReturnsAsIs() {
    String normalXml = "<title>연합뉴스 정상적인 기사 제목입니다.</title>";

    String processedXml = ReflectionTestUtils.invokeMethod(yonhapRssFetcher, "preprocessXml", normalXml);

    assertThat(processedXml).isEqualTo(normalXml);
  }

  @Test
  @DisplayName("XML 전처리 - 파싱 에러를 유발하는 제어 문자가 깔끔하게 제거된다")
  void preprocessXml_ControlCharacters_RemovesThem() {
    String dirtyXml = "<description>연합뉴스\u0000 본문에\u0008 이상한\u001f 문자가 있습니다.</description>";

    String cleanedXml = ReflectionTestUtils.invokeMethod(yonhapRssFetcher, "preprocessXml", dirtyXml);

    assertThat(cleanedXml).isEqualTo("<description>연합뉴스 본문에 이상한 문자가 있습니다.</description>");
  }

  @Test
  @DisplayName("XML 전처리 - null이나 빈 문자열이 들어오면 빈 문자열을 반환한다")
  void preprocessXml_NullOrBlank_ReturnsEmptyString() {
    String nullResult = ReflectionTestUtils.invokeMethod(yonhapRssFetcher, "preprocessXml", (String) null);
    assertThat(nullResult).isEqualTo("");

    String blankResult = ReflectionTestUtils.invokeMethod(yonhapRssFetcher, "preprocessXml", "   ");
    assertThat(blankResult).isEqualTo("");
  }
}
