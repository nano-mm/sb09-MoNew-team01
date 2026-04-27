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
      List<ArticleSource> sourceIn,
      CursorRequest cursorRequest,
      UUID userId
  ) {
    int limit = cursorRequest.limit();
    String orderBy = cursorRequest.orderBy();
    String direction = cursorRequest.direction();

    Article referenceArticle = null;
    if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
      referenceArticle = queryFactory
          .selectFrom(article)
          .where(article.id.eq(UUID.fromString(cursorRequest.cursor())))
          .fetchOne();
    }

    List<Article> content = queryFactory
        .selectDistinct(article)
        .from(article)
        .where(
            keywordContains(condition.keyword()),
            interestId(condition.interestId()),
            sourceIn(sourceIn),
            publishDateBetween(condition.publishDateFrom(), condition.publishDateTo()),
            cursorCondition(referenceArticle, orderBy, direction)
        )
        .orderBy(getOrderSpecifiers(orderBy, direction))
        .limit(limit + 1)
        .fetch();

    boolean hasNext = content.size() > limit;
    if (hasNext) {
      content.remove(limit);
    }

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
        .map(a -> {
          ArticleDto dto = articleMapper.toDto(a);
          return dto.toBuilder()
              .viewedByMe(viewedArticleIds.contains(a.getId()))
              .build();
        })
        .toList();

    Long totalElements = queryFactory
        .select(article.countDistinct())
        .from(article)
        .where(
            keywordContains(condition.keyword()),
            interestId(condition.interestId()),
            sourceIn(sourceIn),
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
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return article.title.containsIgnoreCase(keyword)
        .or(article.summary.containsIgnoreCase(keyword));
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

  private BooleanExpression cursorCondition(Article reference, String orderBy, String direction) {
    if (reference == null) {
      return null;
    }

    UUID refId = reference.getId();

    Instant refDate = reference.getPublishDate();
    boolean isAsc = "ASC".equalsIgnoreCase(direction);

    switch (orderBy) {
      case "viewCount":
        long refView = reference.getViewCount();
        return isAsc ?
            article.viewCount.gt(refView)
                .or(article.viewCount.eq(refView).and(article.publishDate.after(refDate)))
                .or(article.viewCount.eq(refView).and(article.publishDate.eq(refDate)).and(article.id.gt(refId))) :
            article.viewCount.lt(refView)
                .or(article.viewCount.eq(refView).and(article.publishDate.before(refDate)))
                .or(article.viewCount.eq(refView).and(article.publishDate.eq(refDate)).and(article.id.lt(refId)));

      case "commentCount":
        long refComment = reference.getCommentCount();
        return isAsc ?
            article.commentCount.gt(refComment)
                .or(article.commentCount.eq(refComment).and(article.publishDate.after(refDate)))
                .or(article.commentCount.eq(refComment).and(article.publishDate.eq(refDate)).and(article.id.gt(refId))) :
            article.commentCount.lt(refComment)
                .or(article.commentCount.eq(refComment).and(article.publishDate.before(refDate)))
                .or(article.commentCount.eq(refComment).and(article.publishDate.eq(refDate)).and(article.id.lt(refId)));

      default:
        return isAsc ?
            article.publishDate.after(refDate)
                .or(article.publishDate.eq(refDate).and(article.id.gt(refId))) :
            article.publishDate.before(refDate)
                .or(article.publishDate.eq(refDate).and(article.id.lt(refId)));
    }
  }

  private OrderSpecifier<?>[] getOrderSpecifiers(String orderBy, String direction) {
    Order order = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;

    if ("viewCount".equals(orderBy)) {
      return new OrderSpecifier<?>[]{
          new OrderSpecifier<>(order, article.viewCount),
          new OrderSpecifier<>(order, article.publishDate),
          new OrderSpecifier<>(order, article.id)
      };
    } else if ("commentCount".equals(orderBy)) {
      return new OrderSpecifier<?>[]{
          new OrderSpecifier<>(order, article.commentCount),
          new OrderSpecifier<>(order, article.publishDate),
          new OrderSpecifier<>(order, article.id)
      };
    } else {
      return new OrderSpecifier<?>[]{
          new OrderSpecifier<>(order, article.publishDate),
          new OrderSpecifier<>(order, article.id)
      };
    }
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
