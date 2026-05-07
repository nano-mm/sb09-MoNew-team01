package com.monew.application.port.out.persistence;

import com.monew.domain.model.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  List<Comment> findTop10ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

  Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);

  // 물리 삭제 전용 - 테스트에서만 사용, @SQLRestriction 우회하여 삭제된 것도 포함
  @Query(value = "SELECT * FROM comments WHERE id = :id", nativeQuery = true)
  Optional<Comment> findByIdIncludeDeleted(@Param("id") UUID id);

  long countByArticle_IdAndDeletedAtIsNull(UUID articleId);

}