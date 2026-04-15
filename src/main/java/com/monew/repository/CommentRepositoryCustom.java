package com.monew.repository;

import com.monew.entity.Comment;
import java.util.List;

public interface CommentRepositoryCustom {

  List<Comment> findByNewsIdWithCursor(
      Long articleId,
      String sortType, // "LATEST" or "LIKE"
      Object cursor,
      int size
  );
}
