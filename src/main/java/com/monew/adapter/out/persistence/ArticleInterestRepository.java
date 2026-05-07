package com.monew.adapter.out.persistence;

import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {
  @Query("SELECT ai FROM ArticleInterest ai JOIN FETCH ai.interest")
  List<ArticleInterest> findAllWithInterest();

  @Query("SELECT ai FROM ArticleInterest ai JOIN FETCH ai.interest WHERE ai.article IN :articles")
  List<ArticleInterest> findAllByArticleInWithInterest(@Param("articles") List<Article> articles);
}
