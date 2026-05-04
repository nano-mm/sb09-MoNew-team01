package com.monew.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.dto.backup.ArticleBackupDto;
import com.monew.dto.response.ArticleRestoreResultDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleInterest;
import com.monew.entity.Interest;
import com.monew.mapper.ArticleBackupMapper;
import com.monew.repository.ArticleInterestRepository;
import com.monew.repository.InterestRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.storage.backup.ArticleBackupStorage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleBackupService {

  private final ArticleRepository articleRepository;
  private final InterestRepository interestRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final ObjectMapper objectMapper;
  private final ArticleBackupMapper articleBackupMapper;

  private final ArticleBackupStorage articleBackupStorage;

  @Transactional(readOnly = true)
  public void export(LocalDateTime from, LocalDateTime to) throws IOException {

    ZoneId zoneId = ZoneId.of("Asia/Seoul");
    Instant fromInstant = from.atZone(zoneId).toInstant();
    Instant toInstant = to.atZone(zoneId).toInstant();

    log.info("[뉴스 기사] 백업 시작 (기간: {} ~ {})", from, to);

    List<Article> articles = articleRepository.findByPublishDateBetween(fromInstant, toInstant);

    if (articles.isEmpty()){
      log.info("[뉴스 기사] 백업 대상 데이터가 없습니다.");
      return;
    }

    List<ArticleInterest> targetMappings = articleInterestRepository.findAllByArticleInWithInterest(articles);

    Map<UUID, Map<String, List<String>>> articleToInterestsMap = targetMappings.stream()
        .collect(Collectors.groupingBy(
            ai -> ai.getArticle().getId(),
            Collectors.toMap(
                ai -> ai.getInterest().getName(),
                ai -> ai.getInterest().getKeywords() != null ? ai.getInterest().getKeywords() : new ArrayList<>(),
                (existing, replacement) -> existing
            )
        ));

    Map<LocalDate, List<ArticleBackupDto>> groupedByDate = articles.stream()
        .map(article -> articleBackupMapper.toDto(
            article,
            articleToInterestsMap.getOrDefault(article.getId(), Collections.emptyMap())
        ))
        .collect(Collectors.groupingBy(dto -> dto.publishDate().atZone(zoneId).toLocalDate()));


    for (Map.Entry<LocalDate, List<ArticleBackupDto>> entry : groupedByDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<ArticleBackupDto> data = entry.getValue();

      String fileName = "backup_" + date.toString() + ".json";
      String jsonData = objectMapper.writeValueAsString(data);

      articleBackupStorage.saveBackup(fileName, jsonData);
      log.info("[뉴스 기사] 백업 완료: {}", fileName);
    }
  }

  @Transactional
  public ArticleRestoreResultDto importBackup(LocalDateTime from, LocalDateTime to) throws IOException {

    List<Resource> backupResources = articleBackupStorage.loadBackupResources();

    if (backupResources.isEmpty()) {
      log.warn("[뉴스 기사] 백업 경로에 파일이 없습니다.");
      return ArticleRestoreResultDto.builder()
          .restoreDate(Instant.now())
          .restoreArticlesIds(Collections.emptyList())
          .restoredArticleCount(0L)
          .build();
    }

    Instant fromInstant = from.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    Instant toInstant = to.atZone(ZoneId.of("Asia/Seoul")).toInstant();

    Map<String, Interest> interestMap = interestRepository.findAll().stream()
        .collect(Collectors.toMap(Interest::getName, i -> i));

    List<UUID> totalRestoredIds = new ArrayList<>();
    Set<String> urlsAddedInThisBatch = new HashSet<>();

    for (Resource resource : backupResources) {
      List<Article> articlesToSave = new ArrayList<>();
      List<Interest> newInterestsToSave = new ArrayList<>();
      List<ArticleInterest> articleInterestsToSave = new ArrayList<>();

      try (InputStream is = resource.getInputStream()) {
        List<ArticleBackupDto> backupList = objectMapper.readValue(is,
            new TypeReference<List<ArticleBackupDto>>() {});

        List<String> backupUrls = backupList.stream().map(ArticleBackupDto::sourceUrl).toList();
        Set<String> existingInDbUrls = backupUrls.isEmpty() ? Collections.emptySet() : articleRepository.findExistingUrls(backupUrls);

        for (ArticleBackupDto dto : backupList) {
          Instant articleDate = dto.publishDate();
          if (articleDate == null || articleDate.isBefore(fromInstant) || articleDate.isAfter(toInstant)) continue;

          String currentUrl = dto.sourceUrl();
          if (existingInDbUrls.contains(currentUrl) || urlsAddedInThisBatch.contains(currentUrl)) continue;

          Article article = articleBackupMapper.toEntity(dto);
          articlesToSave.add(article);
          urlsAddedInThisBatch.add(currentUrl);

          Map<String, List<String>> keywordsMap = dto.interestKeywords() != null ? dto.interestKeywords() : Collections.emptyMap();

          for (Map.Entry<String, List<String>> entry : keywordsMap.entrySet()) {
            String interestName = entry.getKey();
            List<String> keywords = entry.getValue() != null ? entry.getValue() : new ArrayList<>();

            Interest interest = interestMap.get(interestName);
            if (interest == null) {
              interest = new Interest(interestName, keywords);
              newInterestsToSave.add(interest);
              interestMap.put(interestName, interest);
            }
            articleInterestsToSave.add(ArticleInterest.of(article, interest));
          }
        }
      }

      if (!newInterestsToSave.isEmpty()) {
        interestRepository.saveAll(newInterestsToSave);
      }
      if (!articlesToSave.isEmpty()) {
        articleRepository.saveAll(articlesToSave);
        articleInterestRepository.saveAll(articleInterestsToSave);

        articlesToSave.forEach(a -> totalRestoredIds.add(a.getId()));
      }
    }

    log.info("[뉴스 기사] 데이터 복구 성공: 총 {}개의 기사가 처리되었습니다.", totalRestoredIds.size());

    return ArticleRestoreResultDto.builder()
        .restoreDate(Instant.now())
        .restoreArticlesIds(totalRestoredIds)
        .restoredArticleCount((long) totalRestoredIds.size())
        .build();
  }
}
