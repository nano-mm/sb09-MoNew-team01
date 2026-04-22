package com.monew.service.impl;

import com.monew.dto.response.ArticleViewDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import com.monew.entity.User;
import com.monew.mapper.ArticleViewMapper;
import com.monew.repository.ArticleViewRepository;
import com.monew.repository.UserRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.ArticleViewService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleViewServiceImpl implements ArticleViewService {

  private final ArticleViewRepository articleViewRepository;
  private final ArticleViewMapper articleViewMapper;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;

  @Override
  public ArticleViewDto create(UUID articleId, UUID requestUserId) {

    log.info("[뉴스 기사 뷰] 생성 시작. articleId: {}, requestUserId: {}", articleId, requestUserId);
    Article article = articleRepository.findById(articleId).orElseThrow();
    User user = userRepository.findById(requestUserId).orElseThrow();

    ArticleView newArticleView = ArticleView.builder()
        .article(article)
        .user(user)
        .build();

    articleViewRepository.saveAndFlush(newArticleView);

    log.info("[뉴스 기사 뷰] 생성 완료. articleId: {}, requestUserId: {}", articleId, requestUserId);

    return articleViewMapper.toDto(newArticleView, article);
  }
}
