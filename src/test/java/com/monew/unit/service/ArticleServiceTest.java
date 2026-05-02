package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.monew.client.ArticleFetcher;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleInterest;
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
import com.monew.repository.article.ArticleRepositoryCustom;
import com.monew.service.ArticleService;
import com.monew.service.NotificationService;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleQueryRepository articleQueryRepository;
  @Mock private InterestRepository interestRepository;
  @Mock private ArticleInterestRepository articleInterestRepository;
  @Mock private ArticleMapper articleMapper;
  @Mock private ArticleFetcher mockFetcher;
  @Mock private NotificationService notificationService;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private ArticleRepositoryCustom articleRepositoryCustom;

  private ArticleService articleService;

  @Captor
  private ArgumentCaptor<List<Article>> articleBulkInsertCaptor;

  @Captor
  private ArgumentCaptor<List<ArticleInterest>> mappingBulkInsertCaptor;

  private final UUID ARTICLE_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    articleService = new ArticleService(
        articleRepository,
        articleQueryRepository,
        interestRepository,
        articleRepositoryCustom,
        articleMapper,
        List.of(mockFetcher),
        notificationService,
        subscriptionRepository
    );
  }

  @Test
  @DisplayName("기사 수집 - 중복 URL 처리")
  void collect_FiltersDuplicates() {
    String duplicateUrl = "https://news.test.com/123";
    ArticleDto mockDto = ArticleDto.builder().title("삼성 반도체 뉴스").sourceUrl(duplicateUrl).summary("내용").build();
    Article mockEntity = Article.builder().title("삼성 반도체 뉴스").sourceUrl(duplicateUrl).build();

    Interest mockInterest = new Interest("IT", List.of("반도체"));
    ReflectionTestUtils.setField(mockInterest, "id", UUID.randomUUID());

    given(interestRepository.findAllWithKeywords()).willReturn(List.of(mockInterest));
    given(mockFetcher.fetch(anySet())).willReturn(List.of(mockDto, mockDto)); // 중복 DTO
    given(articleRepository.findExistingUrls(anyList())).willReturn(Set.of());
    given(articleMapper.toEntity(any(ArticleDto.class))).willReturn(mockEntity);

    articleService.collect();

    verify(articleRepositoryCustom).bulkInsertArticle(articleBulkInsertCaptor.capture());

    List<Article> savedArticles = articleBulkInsertCaptor.getValue();

    assertThat(savedArticles).hasSize(1);
  }

  @Test
  @DisplayName("기사 수집 - 수집 중 Fetcher에서 예외가 발생해도 진행")
  void collect_FetcherException_ShouldContinue() {
    Interest mockInterest = new Interest("IT", List.of("인공지능"));
    given(interestRepository.findAllWithKeywords()).willReturn(List.of(mockInterest));

    given(mockFetcher.fetch(anySet())).willThrow(new RuntimeException("API 타임아웃 에러"));

    articleService.collect();

    verify(articleRepositoryCustom, never()).bulkInsertArticle(articleBulkInsertCaptor.capture());
  }

  @Test
  @DisplayName("기사 수집 - 수집된 모든 기사가 이미 DB에 존재")
  void collect_AllDuplicates_ShouldReturnEarly() {
    String duplicateUrl = "https://news.test.com/123";
    ArticleDto mockDto = ArticleDto.builder().title("반도체 뉴스").sourceUrl(duplicateUrl).summary("내용").build();
    Interest mockInterest = new Interest("IT", List.of("반도체"));
    ReflectionTestUtils.setField(mockInterest, "id", UUID.randomUUID());

    given(interestRepository.findAllWithKeywords()).willReturn(List.of(mockInterest));
    given(mockFetcher.fetch(anySet())).willReturn(List.of(mockDto));
    given(articleRepository.findExistingUrls(anyList())).willReturn(Set.of(duplicateUrl));

    articleService.collect();

    verify(articleRepositoryCustom, never()).bulkInsertArticle(anyList());
    verify(articleRepositoryCustom, never()).bulkInsertArticleInterest(anyList());
    verify(notificationService, never()).createNotification(any(), anyString(), any(), any());
  }

  @Test
  @DisplayName("기사 수집 - 기사 저장 후 구독자에게 알림 발송")
  void collect_SendNotification_Success() {
    String newUrl = "https://news.test.com/new";
    ArticleDto mockDto = ArticleDto.builder().title("삼성 반도체 신제품").sourceUrl(newUrl).summary("요약").build();
    Article mockEntity = Article.builder().title("삼성 반도체 신제품").sourceUrl(newUrl).build();

    UUID interestId = UUID.randomUUID();
    Interest mockInterest = new Interest("IT", List.of("반도체"));
    ReflectionTestUtils.setField(mockInterest, "id", interestId);

    UUID subscriberId = UUID.randomUUID();

    given(interestRepository.findAllWithKeywords()).willReturn(List.of(mockInterest));
    given(mockFetcher.fetch(anySet())).willReturn(List.of(mockDto));
    given(articleRepository.findExistingUrls(anyList())).willReturn(Set.of());
    given(articleMapper.toEntity(any(ArticleDto.class))).willReturn(mockEntity);

    given(subscriptionRepository.findUserIdsByInterestId(interestId)).willReturn(List.of(subscriberId));

    articleService.collect();

    verify(articleRepositoryCustom).bulkInsertArticle(anyList());
    verify(articleRepositoryCustom).bulkInsertArticleInterest(anyList());
    verify(notificationService, times(1)).createNotification(
        eq(subscriberId),
        contains("IT"),
        eq(com.monew.entity.enums.ResourceType.INTEREST),
        eq(interestId)
    );
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

    given(articleRepository.findByIdAndDeletedAtIsNull(ARTICLE_ID)).willReturn(Optional.of(mockEntity));
    given(articleMapper.toDto(mockEntity)).willReturn(mockDto);

    ArticleDto result = articleService.find(ARTICLE_ID);

    assertThat(result.title()).isEqualTo("DTO");
  }

  @Test
  @DisplayName("기사 단건 조회 - 실패 (존재하지 않는 ID)")
  void find_Fail_NotFound() {
    given(articleRepository.findByIdAndDeletedAtIsNull(ARTICLE_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> articleService.find(ARTICLE_ID))
        .isInstanceOf(ArticleNotFoundException.class);
  }

  @Test
  @DisplayName("기사 논리 삭제 - 성공")
  void softDelete_Success() {
    Article mockEntity = mock(Article.class);
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(mockEntity));

    articleService.softDelete(ARTICLE_ID);

    verify(mockEntity, times(1)).updateDeletedAt(any(Instant.class));
  }

  @Test
  @DisplayName("기사 논리 삭제 - 실패 (존재하지 않는 기사)")
  void softDelete_Fail_NotFound() {
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

    assertThrows(ArticleNotFoundException.class, () -> {
      articleService.softDelete(ARTICLE_ID);
    });

    verify(articleRepository, never()).save(any());
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
  @DisplayName("기사 물리 삭제 - 실패 (존재하지 않는 기사)")
  void hardDelete_Fail_NotFound() {
    given(articleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

    assertThrows(ArticleNotFoundException.class, () -> {
      articleService.hardDelete(ARTICLE_ID);
    });

    verify(articleRepository, never()).delete(any());
  }

  @Test
  @DisplayName("기사 출처 조회 - 성공")
  void getSources_Success() {
    given(articleQueryRepository.findSources()).willReturn(List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG));

    List<String> result = articleService.getSources();

    assertThat(result).containsExactly("NAVER", "HANKYUNG");
  }
}
