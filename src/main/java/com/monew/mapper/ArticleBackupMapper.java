package com.monew.mapper;

import com.monew.dto.backup.ArticleBackupDto;
import com.monew.entity.Article;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArticleBackupMapper {
  ArticleBackupDto toDto(Article article, Set<String> interestNames);

  @Mapping(target = "id", ignore = true)
  Article toEntity(ArticleBackupDto dto);
}
