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
import java.time.LocalDate;
import java.time.ZoneId;
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
    ZoneId zoneId = ZoneId.of("Asia/Seoul");

    List<Article> articles = articleRepository.findAll();
    List<ArticleInterest> allMappings = articleInterestRepository.findAllWithInterest();

    Map<UUID, Set<String>> articleToInterestsMap = allMappings.stream()
        .collect(Collectors.groupingBy(
            ai -> ai.getArticle().getId(),
            Collectors.mapping(ai -> ai.getInterest().getName(), Collectors.toSet())
        ));

    Map<LocalDate, List<ArticleBackupDto>> groupedByDate = articles.stream()
        .map(article -> new ArticleBackupDto(
            article.getTitle(),
            article.getSummary(),
            article.getSourceUrl(),
            article.getSource().toString(),
            article.getPublishDate(),
            articleToInterestsMap.getOrDefault(article.getId(), Collections.emptySet())
        ))
        .collect(Collectors.groupingBy(dto -> dto.publishDate().atZone(zoneId).toLocalDate()));

    for (Map.Entry<LocalDate, List<ArticleBackupDto>> entry : groupedByDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<ArticleBackupDto> data = entry.getValue();

      String fileName = "backup_" + date.toString() + ".json";
      Resource resource = resourcePatternResolver.getResource(dirPath + fileName);

      if (resource.getURI().getScheme().equals("file")) {
        File parentDir = resource.getFile().getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      }

      if (resource instanceof WritableResource writableResource) {
        try (OutputStream os = writableResource.getOutputStream()) {
          objectMapper.writeValue(os, data);
        }
      }
      log.info("뉴스 기사 백업 완료: {}", fileName);
    }
  }
}
