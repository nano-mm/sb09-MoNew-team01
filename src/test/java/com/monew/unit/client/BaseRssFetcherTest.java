package com.monew.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.monew.dto.response.ArticleDto;
import com.monew.client.ArticleFetchers.BaseRssFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class BaseRssFetcherTest {

  @Mock
  private RestTemplate restTemplate;

  private BaseRssFetcher testFetcher;

  @BeforeEach
  void setUp() {
    testFetcher = new BaseRssFetcher() {
      @Override
      protected String getRssUrl() {
        return "http://dummy.rss.com/feed";
      }

      @Override
      public String getSourceName() {
        return "DUMMY_RSS";
      }
    };

    ReflectionTestUtils.setField(testFetcher, "restTemplate", restTemplate);
  }

  @Test
  @DisplayName("RSS 파싱 및 키워드 매칭 성공 - HTML 태그가 깔끔하게 제거된다")
  void fetch_Success_CleanHtmlAndMatch() {
    String mockXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <item>
              <title>스프링 &lt;b&gt;부트&lt;/b&gt; 업데이트</title>
              <link>http://test.com/1</link>
              <description><![CDATA[<p>새로운 <b>스프링</b> 기능이 추가되었습니다.</p>]]></description>
              <pubDate>Wed, 29 Apr 2026 12:00:00 +0900</pubDate>
            </item>
          </channel>
        </rss>
        """;
    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockXml);

    List<ArticleDto> result = testFetcher.fetch("스프링");

    assertThat(result).hasSize(1);
    ArticleDto dto = result.get(0);
    assertThat(dto.title()).isEqualTo("스프링 부트 업데이트"); // Jsoup 태그 제거 검증
    assertThat(dto.summary()).isEqualTo("새로운 스프링 기능이 추가되었습니다."); // CDATA 및 p태그 제거 검증
    assertThat(dto.sourceUrl()).isEqualTo("http://test.com/1");
    assertThat(dto.source()).isEqualTo("DUMMY_RSS");
  }

  @Test
  @DisplayName("짧은 영단어(3자 이하) 키워드 매칭 - 단어 경계(Word Boundary) 검증")
  void isMatched_ShortEnglishWord() {
    String mockXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <item>
              <title>최신 ai 기술</title>
              <link>http://test.com/1</link>
              <description>혁신</description>
            </item>
            <item>
              <title>main page</title>
              <link>http://test.com/2</link>
              <description>업데이트</description>
            </item>
          </channel>
        </rss>
        """;
    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockXml);

    List<ArticleDto> result = testFetcher.fetch("ai");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("최신 ai 기술");
  }

  @Test
  @DisplayName("키워드가 포함되지 않은 기사는 결과에서 제외된다")
  void fetch_NoMatch_ExcludeFromList() {
    String mockXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Test Feed</title>
            <item>
              <title>전혀 상관없는 뉴스</title>
              <link>http://test.com/1</link>
              <description>날씨가 좋습니다.</description>
            </item>
          </channel>
        </rss>
        """;
    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockXml);

    List<ArticleDto> result = testFetcher.fetch("인공지능");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("RSS 검색 - 응답 XML이 null이거나 비어있으면 빈 리스트 반환")
  void fetch_NullOrBlankXml_ReturnsEmptyList() {
    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(null);
    assertThat(testFetcher.fetch("스프링")).isEmpty();

    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn("   ");
    assertThat(testFetcher.fetch("스프링")).isEmpty();
  }

  @Test
  @DisplayName("RSS 검색 - Entry의 Title, Description, Date가 null이어도 NullPointerException 없이 처리된다")
  void fetch_NullFieldsInEntry_HandledSafely() {
    String mockXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <item>
              <link>http://test.com/1</link>
            </item>
          </channel>
        </rss>
        """;
    given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn(mockXml);

    List<ArticleDto> result = testFetcher.fetch("");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEmpty();
    assertThat(result.get(0).publishDate()).isNotNull();
  }

  @Test
  @DisplayName("외부 API 오류 또는 XML 파싱 실패 시")
  void fetch_Exception_ShouldReturnEmptyList() {
    given(restTemplate.getForObject(anyString(), eq(String.class)))
        .willThrow(new RestClientException("Connection Refused"));

    List<ArticleDto> result = testFetcher.fetch("스프링");

    assertThat(result).isEmpty();
  }
}
