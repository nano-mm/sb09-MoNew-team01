package com.monew.repository;

import com.monew.entity.ArticleView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @EntityGraph(attributePaths = {"article"})
  List<ArticleView> findTop10ByUser_IdOrderByCreatedAtDesc(UUID userId);

  @EntityGraph(attributePaths = {"article"})
  List<ArticleView> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

  Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);
}