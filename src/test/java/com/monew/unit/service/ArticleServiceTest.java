package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.monew.client.ArticleFetcher;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.Interest;
import com.monew.entity.enums.ArticleSource;
import com.monew.exception.article.ArticleNotFoundException;
import com.monew.mapper.ArticleMapper;
import com.monew.repository.ArticleInterestRepository;
import com.monew.repository.ArticleViewRepository;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.repository.article.ArticleQueryRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.NotificationService;
import com.monew.service.impl.ArticleServiceImpl;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleQueryRepository articleQueryRepository;
  @Mock private ArticleViewRepository articleViewRepository;
  @Mock private InterestRepository interestRepository;
  @Mock private ArticleInterestRepository articleInterestRepository;
  @Mock private ArticleMapper articleMapper;
  @Mock private ArticleFetcher mockFetcher;
  @Mock private NotificationService notificationService;
  @Mock private SubscriptionRepository subscriptionRepository;

  private ArticleServiceImpl articleService;

  @Captor
  private ArgumentCaptor<List<Article>> articleListCaptor;

  private final UUID ARTICLE_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    articleService = new ArticleServiceImpl(
        articleRepository,
        articleQueryRepository,
        articleViewRepository,
        interestRepository,
        articleMapper,
        List.of(mockFetcher),
        articleInterestRepository,
        notificationService,
        subscriptionRepository
    );
  }

  @Test
  @DisplayName("기사 수집 - 중복 URL 처리")
  void collect_FiltersDuplicates() {
    String duplicateUrl = "https://news.test.com/123";
    ArticleDto mockDto = ArticleDto.builder().title("test").sourceUrl(duplicateUrl).build();
    Article mockEntity = Article.builder().title("test").build();
    Interest mockInterest = new Interest("IT", List.of("반도체"));

    given(interestRepository.findAllWithKeywords()).willReturn(List.of(mockInterest));
    given(mockFetcher.fetch(anyString())).willReturn(List.of(mockDto));
    given(articleRepository.findExistingUrls(anyList())).willReturn(Set.of());

    given(articleMapper.toEntity(any(ArticleDto.class))).willReturn(mockEntity);

    articleService.collect();
    verify(articleRepository).saveAll(articleListCaptor.capture());
    List<Article> savedArticles = articleListCaptor.getValue();

    assertThat(savedArticles).hasSize(1);
  }

  @Test
  @DisplayName("기사 목록 페이징 조회 - 성공")
  void findArticles_Success() {
    ArticleSearchCondition condition = ArticleSearchCondition.builder().build();
    ArticleSource source = ArticleSource.NAVER;
    CursorRequest cursorRequest = new CursorRequest(null, null, 10, "publishDate", "DESC");
    UUID userId = UUID.randomUUID();

    ArticleDto mockArticle = ArticleDto.builder().title("test").build();

    CursorPageResponseDto<ArticleDto> mockPage = CursorPageResponseDto.<ArticleDto>builder()
        .content(List.of(mockArticle))
        .hasNext(false)
        .build();

    given(articleQueryRepository.searchArticlesByCursor(any(), any(), any(), any()))
        .willReturn(mockPage);

    CursorPageResponseDto<ArticleDto> result = articleService.findArticles(condition,
        Collections.singletonList(source),cursorRequest, userId);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).title()).isEqualTo("test");
  }

  @Test
  @DisplayName("기사 단건 조회 - 성공")
  void find_Success() {
    Article mockEntity = Article.builder().title("entity").build();
    ArticleDto mockDto = ArticleDto.builder().title("DTO").build();

    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(mockEntity));
    given(articleMapper.toDto(mockEntity)).willReturn(mockDto);

    ArticleDto result = articleService.find(ARTICLE_ID);

    assertThat(result.title()).isEqualTo("DTO");
  }

  @Test
  @DisplayName("기사 단건 조회 - 실패 (존재하지 않는 ID)")
  void find_Fail_NotFound() {
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> articleService.find(ARTICLE_ID))
        .isInstanceOf(ArticleNotFoundException.class);
  }

  @Test
  @DisplayName("기사 논리 삭제 - 성공")
  void softDelete_Success() {
    Article mockEntity = mock(Article.class);
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(mockEntity));

    articleService.softDelete(ARTICLE_ID);

    verify(mockEntity, times(1)).markAsDeleted();
  }

  @Test
  @DisplayName("기사 물리 삭제 - 성공")
  void hardDelete_Success() {
    Article mockEntity = Article.builder().id(ARTICLE_ID).build();
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(mockEntity));

    articleService.hardDelete(ARTICLE_ID);

    verify(articleRepository, times(1)).delete(mockEntity);
  }

  @Test
  @DisplayName("기사 출처 조회 - 성공")
  void getSources_Success() {
    given(articleQueryRepository.findSources()).willReturn(List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG));

    List<String> result = articleService.getSources();

    assertThat(result).containsExactly("NAVER", "HANKYUNG");
  }
}
