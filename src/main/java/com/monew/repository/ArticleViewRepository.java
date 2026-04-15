package com.monew.repository;

import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import com.monew.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  ArticleView findByArticleAndUser(Article article, User user);
}
