package com.monew.service.impl;

import com.monew.client.ArticleFetcher;
import com.monew.dto.response.ArticleDto;
import com.monew.entity.Article;
import com.monew.mapper.ArticleMapper;
import com.monew.repository.ArticleRepository;
import com.monew.service.ArticleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleMapper articleMapper;
  private final List<ArticleFetcher> articleFetchers;

  @Override
  public void collect() {

    Set<ArticleDto> articles = new HashSet<>();
    // 키워드 불러오는 작업 필요
    Set<String> allKeywords = Set.of(
        "정부", "시장", "금리", "증시", "투자",
        "발표", "출시", "상승", "하락", "경제",
        "반도체", "인공지능", "부동산", "물가"
    );

    log.info("뉴스 수집 배치 시작. 키워드 갯수: {}", allKeywords.size());

    List<ArticleDto> allFetchedItems = allKeywords.stream()
        .map(this::collectByKeyword)
        .flatMap(Collection::stream)
        .toList();

    List<String> fetchedUrls = allFetchedItems.stream()
        .map(ArticleDto::sourceUrl)
        .toList();

    // 이미 저장된 URL
    Set<String> existingUrlsInDb = articleRepository.findExistingUrls(fetchedUrls);

    List<Article> articleList = new ArrayList<>();

    // 이번 배치 처리 내부의 URL 중복 필터링
    Set<String> urlsInBatch = new HashSet<>();
    for (ArticleDto dto : allFetchedItems) {
      String url = dto.sourceUrl();

      if (urlsInBatch.contains(url) || existingUrlsInDb.contains(url)) {
        continue;
      }

      articleList.add(articleMapper.toEntity(dto));
      urlsInBatch.add(url);
    }

    if (!articleList.isEmpty()) {
      articleRepository.saveAll(articleList);
    }

    log.info("뉴스 기사 수집 완료.  {}개 저장.", articleList.size());
  }

  private List<ArticleDto> collectByKeyword(String keyword) {
    List<ArticleDto> fetchedItems = new ArrayList<>();

    for (ArticleFetcher fetcher : articleFetchers) {
      try {
        fetchedItems.addAll(fetcher.fetch(keyword));
      } catch (Exception e) {
        log.error("[{}] 키워드 수집 중 [{}]에서 에러 발생: {}",
            keyword, fetcher.getClass().getSimpleName(), e.getMessage());
      }
    }

    return fetchedItems;
  }

  @Override
  public ArticleDto find(UUID articleId) {
    Article targetArticle = articleRepository.findById(articleId).orElseThrow();
    return articleMapper.toDto(targetArticle);
  }

  @Override
  public void delete(UUID articleId) {
    // 존재여부 확인 로그 추가 필요
    Article targetArticle = articleRepository.findById(articleId).orElseThrow();

    // 존재 확인 후 삭제
    try {
      articleRepository.deleteById(articleId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isKeywordMatch(ArticleDto dto, String keyword) {
    String content = (dto.title() + dto.summary()).toLowerCase();
    return content.contains(keyword.toLowerCase());
  }
}
