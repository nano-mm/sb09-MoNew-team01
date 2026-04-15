package com.monew.mapper;

import com.monew.dto.response.ArticleDto;
import com.monew.entity.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArticleMapper {
  ArticleDto toDto(Article article);

  @Mapping(target = "commentCount", constant = "0L")
  @Mapping(target = "viewCount", constant = "0L")
  @Mapping(target = "isDeleted", constant = "false")
  Article toEntity(ArticleDto dto);
}
