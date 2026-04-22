package com.monew.mapper;

import com.monew.dto.response.ArticleViewDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {
  @Mapping(source = "articleView.id", target = "id")
  @Mapping(source = "articleView.user.id", target = "viewedBy")
  @Mapping(source = "articleView.createdAt", target = "createdAt")
  @Mapping(source = "article.id", target = "articleId")
  @Mapping(source = "article.source", target = "source")
  @Mapping(source = "article.sourceUrl", target = "sourceUrl")
  @Mapping(source = "article.title", target = "articleTitle")
  @Mapping(source = "article.publishDate", target = "articlePublishedDate")
  @Mapping(source = "article.summary", target = "articleSummary")
  @Mapping(source = "article.commentCount", target = "articleCommentCount")
  @Mapping(source = "article.viewCount", target = "articleViewCount")
  ArticleViewDto toDto(ArticleView articleView, Article article);
}
