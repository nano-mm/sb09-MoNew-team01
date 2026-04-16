package com.monew.repository.impl;

import com.monew.entity.Comment;
import com.monew.entity.QComment;
import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.repository.CommentRepositoryCustom;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private static final QComment comment = QComment.comment;

  @Override
  public List<Comment> findByArticleIdWithCursor(
      String articleId,
      CommentSortType sortType,
      CommentCursor cursor,
      int limit
  ) {
    return queryFactory
        .selectFrom(comment)
        .where(
            comment.articleId.eq(articleId),
            comment.deletedAt.isNull(),       // @SQLRestriction 보완 (명시적 안전장치)
            cursorCondition(sortType, cursor) // 커서 조건 (null이면 첫 페이지)
        )
        .orderBy(orderSpecifiers(sortType))
        .limit(limit)
        .fetch();
  }

  /**
   * 정렬 기준에 따라 커서 조건 생성.
   * cursor가 null이면 첫 페이지이므로 조건 없음.
   *
   * createdAt 기준: (createdAt < lastCreatedAt) OR (createdAt = lastCreatedAt AND id < lastId)
   * likeCount 기준: (likeCount < lastLikeCount) OR (likeCount = lastLikeCount AND id < lastId)
   */
  private BooleanExpression cursorCondition(CommentSortType sortType, CommentCursor cursor) {
    if (cursor == null) {
      return null;
    }

    return switch (sortType) {
      case CREATED_AT -> comment.createdAt.lt(Instant.from(cursor.lastCreatedAt()))
          .or(comment.createdAt.eq(Instant.from(cursor.lastCreatedAt()))
              .and(comment.id.lt(cursor.lastId())));

      case LIKE_COUNT -> comment.likeCount.lt(cursor.lastLikeCount())
          .or(comment.likeCount.eq(cursor.lastLikeCount())
              .and(comment.id.lt(cursor.lastId())));
    };
  }

  /**
   * 정렬 기준: 내림차순(최신/인기순) + id 내림차순(동점 안정 정렬)
   */
  private OrderSpecifier<?>[] orderSpecifiers(CommentSortType sortType) {
    return switch (sortType) {
      case CREATED_AT -> new OrderSpecifier[]{
          comment.createdAt.desc(),
          comment.id.desc()
      };
      case LIKE_COUNT -> new OrderSpecifier[]{
          comment.likeCount.desc(),
          comment.id.desc()
      };
    };
  }
}