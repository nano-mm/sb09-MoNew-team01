package com.monew.mapper;

import com.monew.dto.response.ArticleViewDto;
import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {
  @Mapping(source = "articleView.id", target = "id")
  @Mapping(source = "articleView.user.id", target = "viewedBy")
  @Mapping(source = "article.id", target = "articleId")
  @Mapping(source = "article.title", target = "articleTitle")
  @Mapping(source = "article.publishDate", target = "articlePublishedDate")
  @Mapping(source = "article.summary", target = "articleSummary")
  @Mapping(source = "article.commentCount", target = "articleCommentCount")
  @Mapping(source = "article.viewCount", target = "articleViewCount")
  ArticleViewDto toDto(ArticleView articleView, Article article);
}
