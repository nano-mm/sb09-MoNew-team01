package com.monew.repository;

import com.monew.entity.CommentLike;
import com.monew.entity.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

  boolean existsByComment_IdAndUser_Id(UUID commentId, UUID userId);

  // 물리 삭제 시 댓글에 달린 좋아요 전체 삭제 (cascade 대비 명시적 처리)
  @Modifying
  @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
  void deleteAllByCommentId(@Param("commentId") UUID commentId);

  // 특정 사용자의 좋아요 삭제 (좋아요 취소)
  @Modifying
  @Query("DELETE FROM CommentLike c WHERE c.commentId = :commentId AND c.userId = :userId")
  int deleteByComment_IdAndUser_Id(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

  int deleteCommentLike_AndUser_Id(UUID commentId, UUID userId);
}
