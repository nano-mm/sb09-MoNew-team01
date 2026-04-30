package com.monew.service;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleQueryRepository articleQueryRepository;
  private final InterestRepository interestRepository;

  private final ArticleMapper articleMapper;

  private final List<ArticleFetcher> articleFetchers;

  private final ArticleInterestRepository articleInterestRepository;
  private final NotificationService notificationService;
  private final SubscriptionRepository subscriptionRepository;


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
    log.info("[뉴스 기사] 수집 시작. 키워드 갯수: {}", allKeywords.size());

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
      log.info("[뉴스 기사] 새로 저장할 뉴스 기사가 없습니다.");
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

    // 관심사별로 새로 등록된 기사 수를 집계한 뒤 구독자에게 요약 알림을 한 건만 생성합니다.
    Map<UUID, Integer> interestToCount = new HashMap<>();
    for (Article article : articleList) {
      Set<Interest> interests = urlToInterestsMap.get(article.getSourceUrl());
      if (interests == null) continue;
      for (Interest interest : interests) {
        interestToCount.merge(interest.getId(), 1, Integer::sum);
      }
    }

    if (!interestToCount.isEmpty()) {
      for (Map.Entry<UUID, Integer> e : interestToCount.entrySet()) {
        UUID interestId = e.getKey();
        int count = e.getValue();
        if (count <= 0) continue;

        Interest interest = interestRepository.findById(interestId).orElse(null);
        String interestName = interest != null ? interest.getName() : "";

        List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(interestId);

        for (UUID userId : subscriberIds) {
          notificationService.createNotification(
              userId,
              "[" + interestName + "]와 관련된 기사가 " + count + "건 등록되었습니다.",
              com.monew.entity.enums.ResourceType.INTEREST,
              interestId
          );
        }
      }
    }

    log.info("[뉴스 기사] 수집 완료.  {}개 저장.", articleList.size());
  }

  private List<ArticleDto> collectByKeyword(String keyword) {
    List<ArticleDto> fetchedItems = new ArrayList<>();

    for (ArticleFetcher fetcher : articleFetchers) {
      try {
        fetchedItems.addAll(fetcher.fetch(keyword));
      } catch (Exception e) {
        log.error("[뉴스 기사] [{}] 키워드 수집 중 [{}]에서 에러 발생: {}",
            keyword, fetcher.getClass().getSimpleName(), e.getMessage());
      }
    }

    return fetchedItems;
  }

  @Transactional(readOnly = true)
  public CursorPageResponseDto<ArticleDto> findArticles(
      ArticleSearchCondition condition,
      List<ArticleSource> sourceIn,
      CursorRequest cursorRequest,
      UUID userId) {

    log.info("[뉴스 기사] 조회 시도: userId={}, 검색조건={}, 커서={}", userId, condition, cursorRequest);

    CursorPageResponseDto<ArticleDto> result =
        articleQueryRepository.searchArticlesByCursor(condition, sourceIn, cursorRequest, userId);

    log.info("[뉴스 기사] 조회 완료: userId={}", userId);

    return result;
  }

  @Transactional(readOnly = true)
  public ArticleDto find(UUID articleId) {
    log.info("[뉴스 기사] 단건 조회 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findByIdAndDeletedAtIsNull(articleId).orElseThrow(()
            -> {
          log.warn("[뉴스 기사] 단건 조회 실패: 존재하지 않는 기사 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    log.info("[뉴스 기사] 단건 조회 완료: articleId={}", articleId);
    return articleMapper.toDto(targetArticle);
  }

  @Transactional
  public void softDelete(UUID articleId) {
    log.info("[뉴스 기사] 논리 삭제 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findById(articleId).orElseThrow(()
            -> {
          log.warn("[뉴스 기사] 논리 삭제 실패: 존재하지 않는 기사 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    targetArticle.updateDeletedAt(Instant.now());
    log.info("[뉴스 기사] 논리 삭제 완료: articleId={}", articleId);
  }

  @Transactional
  public void hardDelete(UUID articleId) {
    log.info("[뉴스 기사] 물리 삭제 시도: articleId={}", articleId);
    Article targetArticle = articleRepository.findById(articleId).orElseThrow(()
            -> {
          log.warn("[뉴스 기사] 물리 삭제 실패: 존재하지 않는 기사 ID={}", articleId);
          return new ArticleNotFoundException(articleId);
        }
    );
    articleRepository.delete(targetArticle);
    log.info("[뉴스 기사] 물리 삭제 완료: articleId={}", articleId);
  }

  @Transactional(readOnly = true)
  public List<String> getSources() {
    log.info("[뉴스 기사] 출처 조회 시도");
    List<ArticleSource> sources = articleQueryRepository.findSources();
    log.info("[뉴스 기사] 출처 조회 성공");
    return sources.stream()
        .map(Enum::name)
        .toList();
  }
}
