package com.monew.repository;

import com.monew.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

  boolean existsByUserIdAndCommentId(Long userId, Long commentId);

  void deleteByUserIdAndCommentId(Long userId, Long commentId);

  long countByCommentId(Long commentId);
}
