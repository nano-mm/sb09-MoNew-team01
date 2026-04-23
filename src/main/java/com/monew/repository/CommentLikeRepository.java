package com.monew.repository;

import com.monew.entity.CommentLike;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  @EntityGraph(attributePaths = {"comment", "comment.article", "comment.user"})
  @Query("SELECT cl FROM CommentLike cl WHERE cl.user.id = :userId")
  List<CommentLike> findTop10ByUserIdWithCommentAndUser(@Param("userId") UUID userId, Pageable pageable);

  boolean existsByComment_IdAndUser_Id(UUID commentId, UUID userId);

  // 물리 삭제 시 댓글에 달린 좋아요 전체 삭제
  @Modifying
  @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
  void deleteAllByCommentId(@Param("commentId") UUID commentId);

  // 특정 사용자의 좋아요 삭제 (좋아요 취소) - 삭제 건수 반환
  @Modifying
  @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
  int deleteByComment_IdAndUser_Id(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

  // N+1 방지: 특정 사용자가 좋아요한 댓글 ID 목록 조회
  @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
  List<UUID> findCommentIdsByUserIdAndCommentIdIn(@Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);

}