package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.monew.config.TestQueryDslConfig;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import com.monew.entity.User;
import com.monew.entity.enums.ArticleSource;
import com.monew.mapper.ArticleMapper;
import com.monew.repository.article.ArticleQueryRepository;
import com.monew.repository.article.ArticleRepository;
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
