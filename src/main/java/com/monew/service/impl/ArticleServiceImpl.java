package com.monew.service.impl;

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
import com.monew.repository.article.ArticleQueryRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.ArticleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleQueryRepository articleQueryRepository;
  private final ArticleViewRepository articleViewRepository;
  private final InterestRepository interestRepository;

  private final ArticleMapper articleMapper;

  private final List<ArticleFetcher> articleFetchers;

  private final ArticleInterestRepository articleInterestRepository;

  @Override
  public void collect() {
    List<Interest> allInterests = interestRepository.findAllWithKeywords();

    Map<String, Set<Interest>> keywordToInterestsMap = new HashMap<>();
    for (Interest interest : allInterests) {
      keywordToInterestsMap.computeIfAbsent(interest.getName(), k -> new HashSet<>()).add(interest);
      for (String kw : interest.getKeywords()) {
        keywordToInterestsMap.computeIfAbsent(kw, k -> new HashSet<>()).add(interest);
      }
    }

    Set<String> allKeywords = keywordToInterestsMap.keySet();
    log.info("뉴스 수집 배치 시작. 키워드 갯수: {}", allKeywords.size());

    Map<String, Article> urlToArticleMap = new HashMap<>();
    Map<String, Set<Interest>> urlToInterestsMap = new HashMap<>();

    for (String keyword : allKeywords) {
      List<ArticleDto> fetchedDtos = collectByKeyword(keyword);
      Set<Interest> relatedInterests = keywordToInterestsMap.get(keyword);

      for (ArticleDto dto : fetchedDtos) {
        String url = dto.sourceUrl();
        urlToArticleMap.putIfAbsent(url, articleMapper.toEntity(dto));
        urlToInterestsMap.computeIfAbsent(url, k -> new HashSet<>()).addAll(relatedInterests);
      }
    }

    Set<String> existingUrls = articleRepository.findExistingUrls(new ArrayList<>(urlToArticleMap.keySet()));
    existingUrls.forEach(url -> {
      urlToArticleMap.remove(url);
      urlToInterestsMap.remove(url);
    });

    if (urlToArticleMap.isEmpty()) {
      log.info("새로 저장할 뉴스 기사가 없습니다.");
      return;
    }

    List<Article> articleList = new ArrayList<>(urlToArticleMap.values());
    articleRepository.saveAll(articleList);

    List<ArticleInterest> mappingList = new ArrayList<>();
    for (Article article : articleList) {
      Set<Interest> interests = urlToInterestsMap.get(article.getSourceUrl());
      if (interests != null) {
        for (Interest interest : interests) {
          mappingList.add(ArticleInterest.of(article, interest));
        }
      }
    }

    articleInterestRepository.saveAll(mappingList);

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
  @Transactional(readOnly = true)
  public CursorPageResponseDto<ArticleDto> findArticles(
      ArticleSearchCondition condition,
      CursorRequest cursorRequest,
      UUID userId) {

    log.info("뉴스 기사 조회 시도: userId={}, 검색조건={}", userId, condition);

    CursorPageResponseDto<Article> entityPage =
        articleQueryRepository.searchArticlesByCursor(condition, cursorRequest);

    List<ArticleDto> dtoList = entityPage.content().stream()
        .map(article -> {
          ArticleDto dto = articleMapper.toDto(article);
          // 임시
          return dto.toBuilder().viewedByMe(false).build();
        })
        .toList();

    log.info("뉴스 기사 조회 완료: userId={}", userId);

    return CursorPageResponseDto.<ArticleDto>builder()
        .content(dtoList)
        .nextCursor(entityPage.nextCursor())
        .nextAfter(entityPage.nextAfter())
        .size(entityPage.size())
        .totalElements(entityPage.totalElements())
        .hasNext(entityPage.hasNext())
        .build();
  }

  @Override
  public ArticleDto find(UUID articleId) {
    log.info("뉴스 기사 단건 조회 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findById(articleId).orElseThrow(()
            -> {
          log.warn("뉴스 기사 단건 조회 실패: 존재하지 않는 채널 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    log.info("뉴스 기사 단건 조회 완료: articleId={}", articleId);
    return articleMapper.toDto(targetArticle);
  }

  @Override
  public void softDelete(UUID articleId) {
    log.info("뉴스 기사 논리 삭제 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findById(articleId).orElseThrow(()
        -> {
          log.warn("뉴스 기사 논리 삭제 실패: 존재하지 않는 채널 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    targetArticle.markAsDeleted();
    log.info("뉴스 기사 논리 삭제 완료: articleId={}", articleId);
  }

  @Override
  public void hardDelete(UUID articleId) {
    log.info("뉴스 기사 물리 삭제 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findById(articleId).orElseThrow(()
            -> {
          log.warn("뉴스 기사 물리 삭제 실패: 존재하지 않는 채널 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    articleRepository.delete(targetArticle);
    log.info("뉴스 기사 물리 삭제 완료: articleId={}", articleId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getSources() {
    log.info("뉴스 기사 출처 조회 시도");
    List<ArticleSource> sources = articleQueryRepository.findSources();
    log.info("뉴스 기사 출처 조회 성공");
    return sources.stream()
        .map(Enum::name)
        .toList();
  }

  // 나중에 쓸지도 모름...
  private boolean isKeywordMatch(ArticleDto dto, String keyword) {
    String content = (dto.title() + dto.summary()).toLowerCase();
    return content.contains(keyword.toLowerCase());
  }

  private Set<String> getSearchKeywords() {
    return interestRepository.findAllWithKeywords().stream()
        .flatMap(interest -> {
          return Stream.concat(
              Stream.of(interest.getName()),
              interest.getKeywords().stream()
          );
        })
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }
}
