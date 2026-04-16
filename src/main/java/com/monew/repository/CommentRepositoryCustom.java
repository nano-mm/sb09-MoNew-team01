package com.monew.repository;

import com.monew.entity.Comment;
import java.util.List;

public interface CommentRepositoryCustom {

  List<Comment> findByArticleIdWithCursor(
      Long articleId,
      String sortType, // "LATEST" or "LIKE"
      Object cursor,
      int size
  );
}
