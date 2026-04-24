package com.monew.repository.article;

import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.QArticleInterest;
import com.monew.entity.QArticleView;
import com.monew.entity.enums.ArticleSource;
import com.monew.mapper.ArticleMapper;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.monew.entity.QArticle.article;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepository {

  private final JPAQueryFactory queryFactory;

  private final ArticleMapper articleMapper;

  // 커서기반 조회 용
  public CursorPageResponseDto<ArticleDto> searchArticlesByCursor(
      ArticleSearchCondition condition,
      CursorRequest cursorRequest,
      UUID userId
  ) {

    int limit = cursorRequest.limit();
    String orderBy = cursorRequest.orderBy();
    String direction = cursorRequest.direction();

    List<Article> content = queryFactory
        .selectFrom(article)
        .where(
            keywordContains(condition.keyword()),
            interestId(condition.interestId()),
            sourceIn(condition.sourceIn()),
            publishDateBetween(condition.publishDateFrom(), condition.publishDateTo()),
            cursorCondition(cursorRequest.after(), cursorRequest.cursor(), orderBy, direction)
        )
        .orderBy(getOrderSpecifiers(orderBy, direction))
        .limit(limit + 1)
        .fetch();

    boolean hasNext = content.size() > limit;
    if (hasNext) content.remove(limit);

    String nextCursor = null;
    Instant nextAfter = null;
    if (!content.isEmpty()) {
      Article last = content.get(content.size() - 1);
      nextCursor = last.getId().toString();
      nextAfter = last.getPublishDate();
    }

    List<UUID> articleIds = content.stream().map(Article::getId).toList();
    Set<UUID> viewedArticleIds = getViewedArticleIds(userId, articleIds);

    List<ArticleDto> dtoList = content.stream()
        .map(article -> {
          ArticleDto dto = articleMapper.toDto(article);
          return dto.toBuilder()
              .viewedByMe(viewedArticleIds.contains(article.getId()))
              .build();
        })
        .toList();


    Long totalElements = queryFactory
        .select(article.count())
        .from(article)
        .where(
            keywordContains(condition.keyword()),
            interestId(condition.interestId()),
            sourceIn(condition.sourceIn()),
            publishDateBetween(condition.publishDateFrom(), condition.publishDateTo())
        )
        .fetchOne();

    return CursorPageResponseDto.<ArticleDto>builder()
        .content(dtoList)
        .nextCursor(nextCursor)
        .nextAfter(nextAfter)
        .size(limit)
        .totalElements(totalElements != null ? totalElements : 0L)
        .hasNext(hasNext)
        .build();
  }


  private BooleanExpression keywordContains(String keyword) {
    return (keyword == null || keyword.isBlank()) ? null :
        article.title.contains(keyword).or(article.summary.contains(keyword));
  }

  private BooleanExpression interestId(UUID interestId) {
    if (interestId == null) return null;

    QArticleInterest articleInterest = QArticleInterest.articleInterest;

    return article.id.in(
        JPAExpressions
            .select(articleInterest.article.id)
            .from(articleInterest)
            .where(articleInterest.interest.id.eq(interestId))
    );
  }

  private BooleanExpression sourceIn(List<ArticleSource> sources) {
    return (sources == null || sources.isEmpty()) ? null : article.source.in(sources);
  }

  private BooleanExpression publishDateBetween(LocalDateTime from, LocalDateTime to) {
    ZoneId seoulZone = ZoneId.of("Asia/Seoul");
    Instant fromInstant = from != null ? from.atZone(seoulZone).toInstant() : null;
    Instant toInstant = to != null ? to.atZone(seoulZone).toInstant() : null;

    if (fromInstant != null && toInstant != null) return article.publishDate.between(fromInstant, toInstant);
    if (fromInstant != null) return article.publishDate.goe(fromInstant);
    if (toInstant != null) return article.publishDate.loe(toInstant);
    return null;
  }

  private BooleanExpression cursorCondition(LocalDateTime after, String cursorId, String orderBy, String dir) {
    if (after == null || cursorId == null || cursorId.isBlank()) return null;

    boolean asc = "ASC".equalsIgnoreCase(dir);
    UUID uuid = UUID.fromString(cursorId);
    Instant afterInstant = after.atZone(ZoneId.of("Asia/Seoul")).toInstant();

    return switch (orderBy) {
      case "commentCount" -> {
        yield asc ?
            article.publishDate.gt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.gt(uuid))) :
            article.publishDate.lt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.lt(uuid)));
      }
      case "viewCount" -> asc ?
          article.publishDate.gt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.gt(uuid))) :
          article.publishDate.lt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.lt(uuid)));
      default -> asc ? // publishDate 기준 정렬
          article.publishDate.gt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.gt(uuid))) :
          article.publishDate.lt(afterInstant).or(article.publishDate.eq(afterInstant).and(article.id.lt(uuid)));
    };
  }

  private OrderSpecifier<?>[] getOrderSpecifiers(String orderBy, String dir) {
    Order order = "ASC".equalsIgnoreCase(dir) ? Order.ASC : Order.DESC;
    return switch (orderBy) {
      case "commentCount" -> new OrderSpecifier[]{new OrderSpecifier<>(order, article.commentCount), new OrderSpecifier<>(order, article.id)};
      case "viewCount" -> new OrderSpecifier[]{new OrderSpecifier<>(order, article.viewCount), new OrderSpecifier<>(order, article.id)};
      default -> new OrderSpecifier[]{new OrderSpecifier<>(order, article.publishDate), new OrderSpecifier<>(order, article.id)};
    };
  }

  // 출처 조회
  public List<ArticleSource> findSources() {
    return queryFactory
        .select(article.source)
        .from(article)
        .distinct()
        .fetch();
  }

  // 읽은 기사 조회
  // 조회한 기사를 요청자가 읽었는지 판별하기 위한 메서드
  private Set<UUID> getViewedArticleIds(UUID userId, List<UUID> articleIds) {
    if (userId == null || articleIds.isEmpty()) {
      return Collections.emptySet();
    }

    QArticleView articleView = QArticleView.articleView;

    List<UUID> viewedIds = queryFactory
        .select(articleView.article.id)
        .from(articleView)
        .where(
            articleView.user.id.eq(userId),
            articleView.article.id.in(articleIds)
        )
        .fetch();

    return new HashSet<>(viewedIds);
  }
}
