package com.monew.mapper;

import com.monew.dto.response.ArticleViewDto;
import com.monew.entity.ArticleView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {
  ArticleViewDto toDto(ArticleView articleView);
}
