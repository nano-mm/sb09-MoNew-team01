package com.monew.adapter.out.persistence.article;

import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleInterest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustom {

  private final JdbcTemplate jdbcTemplate;

  public void bulkInsertArticle(List<Article> articles) {
    if (articles == null || articles.isEmpty()) return;

    String articleSql = "INSERT INTO articles (id, title, summary, source_url, publish_date, source) VALUES (?, ?, ?, ?, ?, ?)";

    jdbcTemplate.batchUpdate(articleSql, articles, 1000, (PreparedStatement ps, Article article) -> {
      ps.setObject(1, article.getId());
      ps.setString(2, article.getTitle());
      ps.setString(3, article.getSummary());
      ps.setString(4, article.getSourceUrl());
      ps.setTimestamp(5, Timestamp.from(article.getPublishDate()));
      ps.setString(6, article.getSource().toString());
    });
  }

  public void bulkInsertArticleInterest(List<ArticleInterest> mappings) {
    if (mappings == null || mappings.isEmpty()) return;

    String mappingSql = "INSERT INTO article_interests (id, article_id, interest_id) VALUES (?, ?, ?)";

    jdbcTemplate.batchUpdate(mappingSql, mappings, 1000, (PreparedStatement ps, ArticleInterest ai) -> {
      ps.setObject(1, ai.getId());
      ps.setObject(2, ai.getArticle().getId());
      ps.setObject(3, ai.getInterest().getId());
    });
  }
}
