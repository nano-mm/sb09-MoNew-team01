package com.monew.repository;

import com.monew.entity.ArticleView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @EntityGraph(attributePaths = {"article"})
  @Query("SELECT av FROM ArticleView av WHERE av.user.id = :userId")
  List<ArticleView> findTop10ByUserIdWithArticle(@Param("userId") UUID userId, Pageable pageable);

  @EntityGraph(attributePaths = {"article"})
  List<ArticleView> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
