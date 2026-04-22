package com.monew.repository;

import com.monew.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  // @SQLRestriction 우회 — 논리삭제된 댓글도 조회 (hardDelete용)
  @Query(value = "SELECT * FROM comments WHERE id = :id", nativeQuery = true)
  Optional<Comment> findByIdIncludeDeleted(@Param("id") UUID id);

  List<Comment> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);

  // 특정 기사의 댓글 수 (논리 삭제 제외, @SQLRestriction 자동 적용)
  @Query("SELECT COUNT(c) FROM Comment c WHERE c.article.id = :articleId")
  long countByArticleId(@Param("articleId") UUID articleId);

}