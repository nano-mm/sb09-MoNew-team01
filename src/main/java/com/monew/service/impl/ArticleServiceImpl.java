package com.monew.service.impl;

import com.monew.client.ArticleFetcher;
import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.enums.ArticleSource;
import com.monew.exception.article.ArticleNotFoundException;
import com.monew.mapper.ArticleMapper;
import com.monew.repository.ArticleViewRepository;
import com.monew.repository.article.ArticleQueryRepository;
import com.monew.repository.article.ArticleRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleQueryRepository articleQueryRepository;
  private final ArticleViewRepository articleViewRepository;
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
  @Transactional(readOnly = true)
  public CursorPageResponseDto<ArticleDto> findArticles(ArticleSearchCondition condition
      , CursorRequest cursorRequest, UUID userId) {


    log.info("뉴스 기사 조회 시도: userId={}", userId);
    // 관심사 기능 추가 시 추가 구현 필요
    List<String> interestKeywords = null;

    CursorPageResponseDto<Article> entityPage =
        articleQueryRepository.searchArticlesByCursor(condition, interestKeywords, cursorRequest);

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
    Article targetArticle = articleRepository.findById(articleId).orElseThrow();
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
}
