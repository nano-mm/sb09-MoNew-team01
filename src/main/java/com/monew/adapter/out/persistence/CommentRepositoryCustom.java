package com.monew.adapter.out.persistence;

import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.domain.model.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {

  /**
   * 기사별 댓글 커서 페이지네이션 조회
   *
   * @param articleId 기사 ID
   * @param sortType  정렬 기준 (CREATED_AT | LIKE_COUNT)
   * @param cursor    이전 페이지 마지막 커서 (null이면 첫 페이지)
   * @param limit     조회할 댓글 수
   */
  List<Comment> findByArticleIdWithCursor(
      UUID articleId,
      CommentSortType sortType,
      CommentCursor cursor,
      int limit
  );
}