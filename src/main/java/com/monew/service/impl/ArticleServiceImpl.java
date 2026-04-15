package com.monew.service.impl;

import com.monew.dto.response.ArticleDto;
import com.monew.entity.Article;
import com.monew.mapper.ArticleMapper;
import com.monew.repository.ArticleRepository;
import com.monew.service.ArticleService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

  private final ArticleRepository ariticleRepository;
  private final ArticleMapper articleMapper;

  @Override
  public ArticleDto find(UUID articleId) {
    Article targetArticle = ariticleRepository.findById(articleId).orElseThrow();
    return articleMapper.toDto(targetArticle);
  }

  @Override
  public void delete(UUID articleId) {
    // 존재여부 확인 로그 추가 필요
    Article targetArticle = ariticleRepository.findById(articleId).orElseThrow();

    // 존재 확인 후 삭제
    try {
      ariticleRepository.deleteById(articleId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
