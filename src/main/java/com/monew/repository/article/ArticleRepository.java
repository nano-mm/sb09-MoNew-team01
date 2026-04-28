package com.monew.repository.article;

import com.monew.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
  @Query("SELECT a.sourceUrl FROM Article a WHERE a.sourceUrl IN :urls")
  Set<String> findExistingUrls(@Param("urls") List<String> urls);

  boolean existsBySourceUrl(String url);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :articleId")
  void incrementViewCount(@Param("articleId") UUID articleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Article a SET a.commentCount = a.commentCount + 1 WHERE a.id = :articleId")
  void incrementCommentCount(@Param("articleId") UUID articleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Article a SET a.commentCount = a.commentCount - 1 WHERE a.id = :articleId AND a.commentCount > 0")
  void decrementCommentCount(@Param("articleId") UUID articleId);

  Optional<Article> findByIdAndDeletedAtIsNull(UUID articleId);
}
