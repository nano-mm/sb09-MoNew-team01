package com.monew.application.service;

import com.monew.dto.response.ArticleViewDto;
import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleView;
import com.monew.domain.model.User;
import com.monew.mapper.ArticleViewMapper;
import com.monew.adapter.out.persistence.ArticleViewRepository;
import com.monew.adapter.out.persistence.UserRepository;
import com.monew.adapter.out.persistence.article.ArticleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewService {

  private final ArticleViewRepository articleViewRepository;
  private final ArticleViewMapper articleViewMapper;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final UserActivityReadModelService userActivityReadModelService;

  @Transactional
  public ArticleViewDto create(UUID articleId, UUID requestUserId) {
    Optional<ArticleView> existingView = articleViewRepository.findByArticleIdAndUserId(articleId, requestUserId);

    if (existingView.isPresent()) {
      log.info("[뉴스 기사 뷰] 이미 조회한 기사입니다.");
      Article articleProxy = articleRepository.getReferenceById(articleId);
      return articleViewMapper.toDto(existingView.get(), articleProxy);
    }


    Article article = articleRepository.findById(articleId).orElseThrow();
    User userProxy = userRepository.getReferenceById(requestUserId);

    log.info("[뉴스 기사 뷰] 생성 시작. articleId: {}, requestUserId: {}", articleId, requestUserId);

    ArticleView newArticleView = ArticleView.builder()
        .article(article)
        .user(userProxy)
        .build();

    articleViewRepository.saveAndFlush(newArticleView);

    articleRepository.incrementViewCount(article.getId());

    log.info("[뉴스 기사 뷰] 생성 완료. articleId: {}, requestUserId: {}", articleId, requestUserId);

    userActivityReadModelService.refreshSnapshot(requestUserId);

    return articleViewMapper.toDto(newArticleView, article);
  }


  @Transactional(readOnly = true)
  public List<ArticleViewDto> getRecentArticleViews(UUID userId) {

    List<ArticleView> views = articleViewRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);

    return views.stream()
        .map(view -> articleViewMapper.toDto(view, view.getArticle()))
        .toList();
  }
}