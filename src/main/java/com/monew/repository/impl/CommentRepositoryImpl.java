package com.monew.repository.impl;

import com.monew.entity.Comment;
import com.monew.entity.QComment;
import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.repository.CommentRepositoryCustom;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  private static final QComment qComment = QComment.comment;

  private static final int MAX_LIMIT = 100;

  @Override
  public List<Comment> findByArticleIdWithCursor(
      UUID articleId,
      CommentSortType sortType,
      CommentCursor cursor,
      int limit
  ) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

    return queryFactory
        .selectFrom(qComment)
        .where(
            qComment.article.id.eq(articleId),
            qComment.deletedAt.isNull(),
            cursorCondition(sortType, cursor)
        )
        .orderBy(orderSpecifiers(sortType))
        .limit(safeLimit)
        .fetch();
  }

  private BooleanExpression cursorCondition(CommentSortType sortType, CommentCursor cursor) {
    if (cursor == null) {
      return null;
    }

    return switch (sortType) {

      case CREATED_AT -> qComment.createdAt.lt(cursor.lastCreatedAt())
          .or(qComment.createdAt.eq(cursor.lastCreatedAt())
              .and(qComment.id.lt(cursor.lastId())));

      case LIKE_COUNT -> qComment.likeCount.lt(cursor.lastLikeCount())
          .or(qComment.likeCount.eq(cursor.lastLikeCount())
              .and(qComment.id.lt(cursor.lastId())));
    };
  }

  private OrderSpecifier<?>[] orderSpecifiers(CommentSortType sortType) {
    return switch (sortType) {
      case CREATED_AT -> new OrderSpecifier[]{
          qComment.createdAt.desc(),
          qComment.id.desc()
      };
      case LIKE_COUNT -> new OrderSpecifier[]{
          qComment.likeCount.desc(),
          qComment.id.desc()
      };
    };
  }
}
