package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.monew.dto.response.ArticleViewDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import com.monew.entity.User;
import com.monew.mapper.ArticleViewMapper;
import com.monew.repository.ArticleViewRepository;
import com.monew.repository.UserRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.UserActivityReadModelService;
import com.monew.service.ArticleViewService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {

  @InjectMocks
  private ArticleViewService articleViewService;

  @Mock
  private ArticleViewRepository articleViewRepository;
  @Mock
  private ArticleViewMapper articleViewMapper;
  @Mock
  private ArticleRepository articleRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private UserActivityReadModelService userActivityReadModelService;

  private UUID articleId;
  private UUID userId;
  private Article article;
  private User user;
  private ArticleView articleView;
  private ArticleViewDto articleViewDto;

  @BeforeEach
  void setUp() {
    articleId = UUID.randomUUID();
    userId = UUID.randomUUID();

    user = User.builder()
        .nickname("test")
        .email("test@example.com")
        .password("password@123")
        .build();
    article = Article.builder().id(articleId).title("test article").build();

    articleView = ArticleView.builder()
        .article(article)
        .user(user)
        .build();

    articleViewDto = ArticleViewDto.builder()
        .id(UUID.randomUUID())
        .articleId(articleId)
        .viewedBy(userId)
        .build();
  }

  @Test
  @DisplayName("기사 조회 내역 생성 - 성공")
  void create_Success() {
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

    given(articleViewRepository.saveAndFlush(any(ArticleView.class))).willReturn(articleView);
    given(articleViewMapper.toDto(any(ArticleView.class), eq(article))).willReturn(articleViewDto);

    ArticleViewDto result = articleViewService.create(articleId, userId);

    assertThat(result).isNotNull();
    assertThat(result.articleId()).isEqualTo(articleId);

    verify(articleViewRepository).saveAndFlush(any(ArticleView.class));
    verify(articleRepository).incrementViewCount(articleId);
    verify(userActivityReadModelService).refreshSnapshot(userId);
  }

  @Test
  @DisplayName("기사 조회 내역 생성 실패 - 기사가 존재하지 않음")
  void create_Fail_ArticleNotFound() {
    given(articleRepository.findById(articleId)).willReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () ->
        articleViewService.create(articleId, userId)
    );

    verify(userRepository, never()).findById(any());
    verify(articleViewRepository, never()).saveAndFlush(any());
    verify(articleRepository, never()).incrementViewCount(any());
  }

  @Test
  @DisplayName("특정 유저의 최근 기사 조회 내역 10개 반환")
  void getRecentArticleViews_Success() {
    List<ArticleView> views = List.of(articleView);
    given(articleViewRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId))
        .willReturn(views);
    given(articleViewMapper.toDto(articleView, article)).willReturn(articleViewDto);

    List<ArticleViewDto> result = articleViewService.getRecentArticleViews(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).viewedBy()).isEqualTo(userId);
  }
}
