package com.monew.mapper;

import com.monew.dto.response.ArticleDto;
import com.monew.entity.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleMapper {
  ArticleDto toDto(Article article);
}
