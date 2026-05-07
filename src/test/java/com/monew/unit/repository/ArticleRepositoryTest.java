package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.monew.config.TestQueryDslConfig;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleInterest;
import com.monew.domain.model.ArticleView;
import com.monew.domain.model.Interest;
import com.monew.domain.model.User;
import com.monew.domain.model.enums.ArticleSource;
import com.monew.mapper.ArticleMapper;
import com.monew.application.port.out.persistence.article.ArticleQueryRepository;
import com.monew.application.port.out.persistence.article.ArticleRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({TestQueryDslConfig.class, ArticleQueryRepository.class})
class ArticleRepositoryTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ArticleQueryRepository articleQueryRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @MockitoBean
  private ArticleMapper articleMapper;

  private User testUser;
  private Article article1;
  private Article article2;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .nickname("test")
        .email("test@example.com")
        .password("password@123")
        .build();
    em.persist(testUser);

    article1 = Article.builder()
        .title("test")
        .summary("test")
        .source(ArticleSource.NAVER)
        .sourceUrl("testurl1")
        .publishDate(Instant.now().minusSeconds(1000))
        .build();

    article2 = Article.builder()
        .title("test2")
        .summary("it")
        .source(ArticleSource.CHOSUN)
        .sourceUrl("testurl2")
        .publishDate(Instant.now())
        .build();

    em.persist(article1);
    em.persist(article2);

    ArticleView articleView = ArticleView.builder()
        .article(article2)
        .user(testUser)
        .build();

    em.persist(articleView);

    em.flush();
    em.clear();

    given(articleMapper.toDto(any(Article.class))).willAnswer(invocation -> {
      Article arg = invocation.getArgument(0);
      return ArticleDto.builder()
          .id(arg.getId())
          .title(arg.getTitle())
          .build();
    });
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회")
  void searchArticlesByCursor() {
    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .keyword("it")
        .build();

    ArticleSource source = ArticleSource.CHOSUN;

    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, Collections.singletonList(source), cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);

    ArticleDto resultDto = response.content().get(0);
    assertThat(resultDto.title()).isEqualTo("test2");

    assertThat(resultDto.viewedByMe()).isTrue();
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 커서가 존재하고 조회순 정렬일 때")
  void searchArticlesByCursor_WithCursorAndViewCountOrder() {
    ReflectionTestUtils.setField(article1, "viewCount", 1L);
    ReflectionTestUtils.setField(article2, "viewCount", 2L);

    em.merge(article1);
    em.merge(article2);
    em.flush();
    em.clear();

    ArticleSearchCondition condition = ArticleSearchCondition.builder().build();

    CursorRequest cursorRequest = new CursorRequest(article2.getId().toString(), null, 10, "viewCount", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).id()).isEqualTo(article1.getId());
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 날짜 필터링 적용")
  void searchArticlesByCursor_WithDateFilter() {
    LocalDateTime from = LocalDateTime.now().minusMinutes(5);
    LocalDateTime to = LocalDateTime.now().plusMinutes(5);

    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .publishDateFrom(from)
        .publishDateTo(to)
        .build();

    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).title()).isEqualTo("test2");
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 관심사(Interest) 필터링 적용")
  void searchArticlesByCursor_WithInterestFilter() {
    Interest interest = new Interest("경제", List.of("주식"));
    em.persist(interest);

    ArticleInterest articleInterest = ArticleInterest.of(article1, interest);
    em.persist(articleInterest);
    em.flush();
    em.clear();

    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .interestId(interest.getId())
        .build();

    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).id()).isEqualTo(article1.getId());
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 다음 페이지가 존재하는 경우")
  void searchArticlesByCursor_HasNextTrue() {
    ArticleSearchCondition condition = ArticleSearchCondition.builder().build();

    CursorRequest cursorRequest = new CursorRequest(null, null, 1, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 검색 결과가 없는 경우")
  void searchArticlesByCursor_EmptyResult() {
    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .keyword("11111111")
        .build();
    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 댓글순, 오름차순 정렬")
  void searchArticlesByCursor_WithCursorAndCommentCountAsc() {
    ReflectionTestUtils.setField(article1, "commentCount", 10L);
    ReflectionTestUtils.setField(article2, "commentCount", 20L);
    em.merge(article1);
    em.merge(article2);
    em.flush();
    em.clear();

    ArticleSearchCondition condition = ArticleSearchCondition.builder().build();

    CursorRequest cursorRequest = new CursorRequest(article1.getId().toString(), null, 10, "commentCount", "ASC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).id()).isEqualTo(article2.getId()); // article2가 나와야 함
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 발행일 기준 정렬, 내림차순")
  void searchArticlesByCursor_WithCursorAndPublishDateDesc() {
    ArticleSearchCondition condition = ArticleSearchCondition.builder().build();

    CursorRequest cursorRequest = new CursorRequest(article2.getId().toString(), null, 10, "publishDate", "DESC");

    CursorPageResponseDto<ArticleDto> response =
        articleQueryRepository.searchArticlesByCursor(condition, null, cursorRequest, testUser.getId());

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).id()).isEqualTo(article1.getId());
  }

  @Test
  @DisplayName("커서 기반 뉴스 기사 조회 - 날짜 필터링")
  void searchArticlesByCursor_PartialDateFilter() {
    LocalDateTime now = LocalDateTime.now();
    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");

    ArticleSearchCondition conditionFrom = ArticleSearchCondition.builder()
        .publishDateFrom(now.minusMinutes(5))
        .build();
    CursorPageResponseDto<ArticleDto> responseFrom =
        articleQueryRepository.searchArticlesByCursor(conditionFrom, null, cursorRequest, testUser.getId());
    assertThat(responseFrom.content()).isNotEmpty();

    ArticleSearchCondition conditionTo = ArticleSearchCondition.builder()
        .publishDateTo(now.plusMinutes(5))
        .build();
    CursorPageResponseDto<ArticleDto> responseTo =
        articleQueryRepository.searchArticlesByCursor(conditionTo, null, cursorRequest, testUser.getId());
    assertThat(responseTo.content()).isNotEmpty();
  }

  @Test
  @DisplayName("활성화된 기사들의 출처(Source) 목록 조회")
  void findSources_Success() {
    List<ArticleSource> sources = articleQueryRepository.findSources();

    assertThat(sources).hasSize(2);
    assertThat(sources).containsExactlyInAnyOrder(ArticleSource.NAVER, ArticleSource.CHOSUN);
  }

  @Test
  @DisplayName("DB에 존재하는 URL 조회")
  void findExistingUrls_Success() {
    List<String> targetUrls = List.of(
        "testurl1", // 존재하는 URL
        "fakeurl" // 존재하지 않는 URL
    );

    Set<String> existingUrls = articleRepository.findExistingUrls(targetUrls);

    assertThat(existingUrls).hasSize(1);
    assertThat(existingUrls).containsExactly("testurl1");
  }

  @Test
  @DisplayName("특정 URL의 기사 존재 여부")
  void existsBySourceUrl_Success() {
    assertThat(articleRepository.existsBySourceUrl("testurl2")).isTrue();
    assertThat(articleRepository.existsBySourceUrl("fakeurl")).isFalse();
  }

  @Test
  @DisplayName("기사 조회수 증가")
  void incrementViewCount_Success() {
    articleRepository.incrementViewCount(article1.getId());

    Article updatedArticle = articleRepository.findById(article1.getId()).orElseThrow();
    assertThat(updatedArticle.getViewCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("기사 댓글 수 증가")
  void incrementCommentCount_Success() {
    articleRepository.incrementCommentCount(article2.getId());

    Article updatedArticle = articleRepository.findById(article2.getId()).orElseThrow();
    assertThat(updatedArticle.getCommentCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("기사 댓글 수 감소")
  void decrementCommentCount_SafetyCheck() {
    articleRepository.incrementCommentCount(article1.getId());

    articleRepository.decrementCommentCount(article1.getId());
    articleRepository.decrementCommentCount(article2.getId());

    // 정상
    Article updatedArticle1 = articleRepository.findById(article1.getId()).orElseThrow();

    // 감소되면 안됨
    Article updatedArticle2 = articleRepository.findById(article2.getId()).orElseThrow();

    assertThat(updatedArticle1.getCommentCount()).isEqualTo(0L);
    assertThat(updatedArticle2.getCommentCount()).isEqualTo(0L);
  }
}
