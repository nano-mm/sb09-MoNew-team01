package com.monew.repository;

import com.monew.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  // @SQLRestriction 이 자동으로 deleted_at IS NULL 조건을 추가하므로
  // 아래 메서드들은 모두 활성 댓글만 조회함

  // 물리 삭제 전용 - 테스트에서만 사용, @SQLRestriction 우회하여 삭제된 것도 포함
  @Query(value = "SELECT * FROM comments WHERE id = :id", nativeQuery = true)
  Optional<Comment> findByIdIncludingDeleted(@Param("id") UUID id);

  // 물리 삭제 (연관된 likes는 DB CASCADE 또는 서비스에서 먼저 삭제)
  @Modifying
  @Query(value = "DELETE FROM comments WHERE id = :id", nativeQuery = true)
  void hardDeleteById(@Param("id") UUID id);

  // 특정 기사의 댓글 수 (논리 삭제 제외)
  long countByArticleId(String articleId);
}
