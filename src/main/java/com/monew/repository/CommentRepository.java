package com.monew.repository;

import com.monew.entity.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  List<Comment> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);

  // 물리 삭제 전용 - 테스트에서만 사용, @SQLRestriction 우회하여 삭제된 것도 포함
  @Query(value = "SELECT * FROM comments WHERE id = :id", nativeQuery = true)
  Optional<Comment> findByIdIncludeDeleted(@Param("id") UUID id);

  List<Comment> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);

  // 특정 기사의 댓글 수 (논리 삭제 제외, @SQLRestriction 자동 적용)
  @Query("SELECT COUNT(c) FROM Comment c WHERE c.article.id = :articleId")
  long countByArticleId(@Param("articleId") UUID articleId);

}