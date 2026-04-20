package com.monew.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.dto.backup.ArticleBackupDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleInterest;
import com.monew.entity.Interest;
import com.monew.repository.ArticleInterestRepository;
import com.monew.repository.InterestRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.ArticleBackupService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleBackupServiceImpl implements ArticleBackupService {

  private final ArticleRepository articleRepository;
  private final InterestRepository interestRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final ObjectMapper objectMapper;
  
  private final ResourcePatternResolver resourcePatternResolver;

  @Value("${app.backup.dir}")
  private String backupDir;
  
  @Transactional(readOnly = true)
  public void export() throws IOException {
    String dirPath = backupDir.endsWith("/") ? backupDir : backupDir + "/";
    
    String fileName = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
    String fullPath = dirPath + fileName;

    Resource resource = resourcePatternResolver.getResource(fullPath);
    
    if (resource.getURI().getScheme().equals("file")) {
      File file = resource.getFile();
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs();
      }
    }

    List<Article> articles = articleRepository.findAll();

    List<ArticleInterest> allMappings = articleInterestRepository.findAllWithInterest();

    Map<UUID, Set<String>> articleToInterestsMap = allMappings.stream()
        .collect(Collectors.groupingBy(
            ai -> ai.getArticle().getId(),
            Collectors.mapping(ai -> ai.getInterest().getName(), Collectors.toSet())
        ));

    List<ArticleBackupDto> backupList = articles.stream()
        .map(article -> new ArticleBackupDto(
            article.getTitle(),
            article.getSummary(),
            article.getSourceUrl(),
            article.getSource().toString(),
            article.getPublishDate(),
            articleToInterestsMap.getOrDefault(article.getId(), Collections.emptySet())
        ))
        .toList();

    if (resource instanceof WritableResource writableResource) {
      try (OutputStream os = writableResource.getOutputStream()) {
        objectMapper.writeValue(os, backupList);
      }
    } else {
      objectMapper.writeValue(resource.getFile(), backupList);
    }

    log.info("새 백업 파일 생성 성공: {}", fullPath);
  }
}
