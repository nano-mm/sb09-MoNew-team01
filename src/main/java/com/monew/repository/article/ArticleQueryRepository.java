package com.monew.repository.article;

import com.monew.dto.request.ArticleSearchCondition;
import com.monew.dto.request.CursorRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.enums.ArticleSource;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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

  public CursorPageResponseDto<Article> searchArticlesByCursor(
      ArticleSearchCondition condition,
      List<String> interestKeywords,
      CursorRequest cursorRequest) {

    int limit = cursorRequest.limit() != null ? cursorRequest.limit() : 20;
    String orderBy = cursorRequest.orderBy() != null ? cursorRequest.orderBy() : "publishDate";
    String direction = cursorRequest.direction() != null ? cursorRequest.direction() : "DESC";

    Article cursorArticle = null;
    if (cursorRequest.cursor() != null && !cursorRequest.cursor().isBlank()) {
      cursorArticle = queryFactory
          .selectFrom(article)
          .where(article.id.eq(UUID.fromString(cursorRequest.cursor())))
          .fetchOne();
    }

    List<Article> content = queryFactory
        .selectFrom(article)
        .where(
            keywordContains(condition.keyword()),            // 제목/요약 부분일치
            interestKeywordsContains(interestKeywords),     // 관심사 키워드 검색
            sourceIn(condition.sourceIn()),                  // 출처 필터
            publishDateBetween(condition.publishDateFrom(), condition.publishDateTo()), // 날짜 범위
            cursorCondition(cursorArticle, orderBy, direction) // 동적 커서 로직
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

    Long totalElements = queryFactory
        .select(article.count())
        .from(article)
        .where(
            keywordContains(condition.keyword()),
            interestKeywordsContains(interestKeywords),
            sourceIn(condition.sourceIn()),
            publishDateBetween(condition.publishDateFrom(), condition.publishDateTo())
        )
        .fetchOne();

    return CursorPageResponseDto.<Article>builder()
        .content(content)
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

  private BooleanExpression interestKeywordsContains(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) return null;
    BooleanExpression expression = null;
    for (String kw : keywords) {
      BooleanExpression match = article.title.contains(kw).or(article.summary.contains(kw));
      expression = (expression == null) ? match : expression.or(match);
    }
    return expression;
  }

  private BooleanExpression sourceIn(List<ArticleSource> sources) {
    return (sources == null || sources.isEmpty()) ? null : article.source.in(sources);
  }

  private BooleanExpression publishDateBetween(LocalDateTime from, LocalDateTime to) {
    Instant fromInstant = from != null ? from.atZone(ZoneId.systemDefault()).toInstant() : null;
    Instant toInstant = to != null ? to.atZone(ZoneId.systemDefault()).toInstant() : null;

    if (fromInstant != null && toInstant != null) return article.publishDate.between(fromInstant, toInstant);
    if (fromInstant != null) return article.publishDate.goe(fromInstant);
    if (toInstant != null) return article.publishDate.loe(toInstant);
    return null;
  }

  private BooleanExpression cursorCondition(Article cursor, String orderBy, String dir) {
    if (cursor == null) return null;
    boolean asc = "ASC".equalsIgnoreCase(dir);

    return switch (orderBy) {
      case "commentCount" -> asc ?
          article.commentCount.gt(cursor.getCommentCount())
              .or(article.commentCount.eq(cursor.getCommentCount()).and(article.id.gt(cursor.getId()))) :
          article.commentCount.lt(cursor.getCommentCount())
              .or(article.commentCount.eq(cursor.getCommentCount()).and(article.id.lt(cursor.getId())));

      case "viewCount" -> asc ?
          article.viewCount.gt(cursor.getViewCount())
              .or(article.viewCount.eq(cursor.getViewCount()).and(article.id.gt(cursor.getId()))) :
          article.viewCount.lt(cursor.getViewCount())
              .or(article.viewCount.eq(cursor.getViewCount()).and(article.id.lt(cursor.getId())));

      default -> asc ?
          article.publishDate.gt(cursor.getPublishDate())
              .or(article.publishDate.eq(cursor.getPublishDate()).and(article.id.gt(cursor.getId()))) :
          article.publishDate.lt(cursor.getPublishDate())
              .or(article.publishDate.eq(cursor.getPublishDate()).and(article.id.lt(cursor.getId())));
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
}
