package com.monew.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.application.port.in.ArticleBackupUseCase;
import com.monew.dto.backup.ArticleBackupDto;
import com.monew.dto.response.ArticleRestoreResultDto;
import com.monew.domain.model.Article;
import com.monew.domain.model.ArticleInterest;
import com.monew.domain.model.Interest;
import com.monew.mapper.ArticleBackupMapper;
import com.monew.application.port.out.persistence.ArticleInterestRepository;
import com.monew.application.port.out.persistence.InterestRepository;
import com.monew.application.port.out.persistence.article.ArticleRepository;
import com.monew.application.port.out.persistence.article.ArticleRepositoryCustom;
import com.monew.application.port.out.storage.backup.ArticleBackupStorage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleBackupService implements ArticleBackupUseCase {

  private final ArticleRepository articleRepository;
  private final InterestRepository interestRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final ArticleRepositoryCustom articleRepositoryCustom;
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
    List<Resource> backupResources = articleBackupStorage.loadBackupResources(from, to);

    log.info("[뉴스 기사] 복구 데이터 불러오기 완료: 파일 갯수: {}", backupResources.size());

    if (backupResources.isEmpty()) {
      return emptyRestoreResult();
    }
    Map<String, Interest> interestMap = interestRepository.findAll().stream()
        .collect(Collectors.toMap(Interest::getName, i -> i));

    List<UUID> totalRestoredIds = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();

    for (Resource resource : backupResources) {
      processBackupFile(resource, interestMap, processedUrls, totalRestoredIds);
    }

    log.info("[뉴스 기사] 데이터 복구 성공: 총 {}개의 기사가 처리되었습니다.", totalRestoredIds.size());

    return ArticleRestoreResultDto.builder()
        .restoreDate(Instant.now())
        .restoreArticlesIds(totalRestoredIds)
        .restoredArticleCount((long) totalRestoredIds.size())
        .build();
  }

  private void processBackupFile(Resource resource, Map<String, Interest> interestMap,
      Set<String> processedUrls, List<UUID> totalRestoredIds) throws IOException {

    List<Article> articlesToSave = new ArrayList<>();
    List<Interest> newInterestsToSave = new ArrayList<>();
    List<ArticleInterest> articleInterestsToSave = new ArrayList<>();

    try (InputStream is = resource.getInputStream()) {
      List<ArticleBackupDto> backupList = objectMapper.readValue(is, new TypeReference<List<ArticleBackupDto>>() {});

      List<String> backupUrls = backupList.stream().map(ArticleBackupDto::sourceUrl).toList();
      Set<String> existingInDbUrls = backupUrls.isEmpty() ? Collections.emptySet() : articleRepository.findExistingUrls(backupUrls);

      for (ArticleBackupDto dto : backupList) {
        String currentUrl = dto.sourceUrl();
        if (existingInDbUrls.contains(currentUrl) || processedUrls.contains(currentUrl)) continue;

        Article article = articleBackupMapper.toEntity(dto);
        article.generateIdForBulkInsert();
        articlesToSave.add(article);
        processedUrls.add(currentUrl);

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
          ArticleInterest newArticleInterest = ArticleInterest.of(article, interest);
          newArticleInterest.generateIdForBulkInsert();
          articleInterestsToSave.add(newArticleInterest);
        }
      }
    }

    saveToDatabase(articlesToSave, newInterestsToSave, articleInterestsToSave);

    articlesToSave.forEach(a -> totalRestoredIds.add(a.getId()));
  }

  private void saveToDatabase(List<Article> articles, List<Interest> newInterests, List<ArticleInterest> mappings) {

    if (!newInterests.isEmpty()) {
      interestRepository.saveAllAndFlush(newInterests);
    }

    if (!articles.isEmpty()) {
      articleRepositoryCustom.bulkInsertArticle(articles);
    }

    if (!mappings.isEmpty()) {
      articleRepositoryCustom.bulkInsertArticleInterest(mappings);
    }
  }

  private ArticleRestoreResultDto emptyRestoreResult() {
    return ArticleRestoreResultDto.builder()
        .restoreDate(Instant.now())
        .restoreArticlesIds(Collections.emptyList())
        .restoredArticleCount(0L)
        .build();
  }
}
