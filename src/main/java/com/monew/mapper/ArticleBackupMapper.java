package com.monew.mapper;

import com.monew.dto.backup.ArticleBackupDto;
import com.monew.domain.model.Article;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArticleBackupMapper {
  @Mapping(source = "article.title", target = "title")
  @Mapping(source = "article.summary", target = "summary")
  @Mapping(source = "article.sourceUrl", target = "sourceUrl")
  @Mapping(source = "article.source", target = "source")
  @Mapping(source = "article.publishDate", target = "publishDate")
  @Mapping(source = "article.deletedAt", target = "deletedAt")
  @Mapping(source = "interestKeywords", target = "interestKeywords")
  ArticleBackupDto toDto(Article article, Map<String, List<String>> interestKeywords);

  @Mapping(target = "id", ignore = true)
  Article toEntity(ArticleBackupDto dto);
}
