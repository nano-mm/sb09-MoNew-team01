package com.monew.repository.article;

import com.monew.entity.Article;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
  @Query("SELECT a.sourceUrl FROM Article a WHERE a.sourceUrl IN :urls")
  Set<String> findExistingUrls(@Param("urls") List<String> urls);
}
