package com.monew.unit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.dto.backup.ArticleBackupDto;
import com.monew.dto.response.ArticleRestoreResultDto;
import com.monew.domain.model.Article;
import com.monew.domain.model.Interest;
import com.monew.mapper.ArticleBackupMapper;
import com.monew.adapter.out.persistence.ArticleInterestRepository;
import com.monew.adapter.out.persistence.InterestRepository;
import com.monew.adapter.out.persistence.article.ArticleRepository;
import com.monew.adapter.out.persistence.article.ArticleRepositoryCustom;
import com.monew.application.service.ArticleBackupService;
import com.monew.adapter.out.storage.backup.ArticleBackupStorage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArticleBackupServiceTest {

  @InjectMocks
  private ArticleBackupService backupService;

  @Mock private ArticleRepository articleRepository;
  @Mock private InterestRepository interestRepository;
  @Mock private ArticleInterestRepository articleInterestRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private ArticleBackupMapper articleBackupMapper;

  @Mock private ArticleRepositoryCustom articleRepositoryCustom;
  @Mock private ArticleBackupStorage articleBackupStorage;

  @Captor
  private ArgumentCaptor<List<Article>> articleListCaptor;

  private final LocalDateTime exportFrom = LocalDateTime.of(2026, 4, 1, 0, 0);
  private final LocalDateTime exportTo = LocalDateTime.of(2026, 4, 30, 23, 59);

  @Test
  @DisplayName("뉴스 기사 백업 - 지정된 기간 내 성공")
  void export_success() throws Exception {
    Article mockArticle = Article.builder().id(UUID.randomUUID()).build();

    given(articleRepository.findByPublishDateBetween(any(Instant.class), any(Instant.class)))
        .willReturn(List.of(mockArticle));

    given(articleInterestRepository.findAllByArticleInWithInterest(anyList()))
        .willReturn(List.of());

    ArticleBackupDto mockDto = ArticleBackupDto.builder()
        .publishDate(Instant.now())
        .build();
    given(articleBackupMapper.toDto(any(), any())).willReturn(mockDto);

    String dummyJson = "[{\"title\":\"test\"}]";
    given(objectMapper.writeValueAsString(any())).willReturn(dummyJson);

    backupService.export(exportFrom, exportTo);

    verify(articleBackupStorage).saveBackup(anyString(), anyString());
  }

  @Test
  @DisplayName("뉴스 기사 백업 - 저장된 기사가 없는 경우 조기 종료")
  void export_EmptyArticles_ShouldReturnEarly() throws Exception {
    given(articleRepository.findByPublishDateBetween(any(Instant.class), any(Instant.class)))
        .willReturn(Collections.emptyList());

    backupService.export(exportFrom, exportTo);

    verify(articleBackupStorage, never()).saveBackup(anyString(), anyString());
  }

  @Test
  @DisplayName("뉴스 기사 백업 - 발행일이 다른 기사는 각각 분리되어 여러 파일로 저장됨")
  void export_GroupByDate_CreatesMultipleFiles() throws Exception {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Article article1 = Article.builder().id(id1).build();
    Article article2 = Article.builder().id(id2).build();

    given(articleRepository.findByPublishDateBetween(any(Instant.class), any(Instant.class)))
        .willReturn(List.of(article1, article2));
    given(articleInterestRepository.findAllByArticleInWithInterest(anyList()))
        .willReturn(Collections.emptyList());

    ArticleBackupDto dto1 = ArticleBackupDto.builder()
        .publishDate(LocalDateTime.of(2026, 4, 1, 0, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant())
        .build();
    ArticleBackupDto dto2 = ArticleBackupDto.builder()
        .publishDate(LocalDateTime.of(2026, 4, 2, 0, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant())
        .build();

    given(articleBackupMapper.toDto(eq(article1), any())).willReturn(dto1);
    given(articleBackupMapper.toDto(eq(article2), any())).willReturn(dto2);
    given(objectMapper.writeValueAsString(any())).willReturn("[]");

    backupService.export(exportFrom, exportTo);

    verify(articleBackupStorage, times(2)).saveBackup(anyString(), anyString());
  }

  @Test
  @DisplayName("뉴스 기사 복구")
  void importBackup_success() throws Exception {

    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

    Instant articleInstant = LocalDateTime.of(2026, 4, 15, 12, 0)
        .atZone(ZoneId.of("Asia/Seoul"))
        .toInstant();

    Resource mockResource = mock(Resource.class);
    InputStream mockInputStream = new ByteArrayInputStream("[]".getBytes());
    given(mockResource.getInputStream()).willReturn(mockInputStream);

    given(articleBackupStorage.loadBackupResources(from, to)).willReturn(List.of(mockResource));

    ArticleBackupDto mockDto = ArticleBackupDto.builder()
        .title("test")
        .sourceUrl("http://test.com")
        .publishDate(articleInstant)
        .interestKeywords(Map.of("IT", List.of("인공지능", "출시", "반도체")))
        .build();

    given(objectMapper.readValue(any(InputStream.class), ArgumentMatchers.<TypeReference<List<ArticleBackupDto>>>any()))
        .willReturn(List.of(mockDto));

    given(articleRepository.findExistingUrls(anyList())).willReturn(Collections.emptySet());

    UUID expectedId = UUID.randomUUID();
    Article mockArticle = Article.builder()
        .id(expectedId)
        .title("test")
        .build();
    given(articleBackupMapper.toEntity(mockDto)).willReturn(mockArticle);

    Interest mockInterest = new Interest("IT", List.of("인공지능", "출시", "반도체"));
    given(interestRepository.findAll()).willReturn(List.of(mockInterest));

    ArticleRestoreResultDto result = backupService.importBackup(from, to);

    verify(articleRepositoryCustom).bulkInsertArticle(anyList());
  }

  @Test
  @DisplayName("뉴스 기사 복구 - 백업 파일이 존재하지 않는 경우")
  void importBackup_NoResources_ShouldReturnEarly() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);

    given(articleBackupStorage.loadBackupResources(from, to)).willReturn(Collections.emptyList());

    ArticleRestoreResultDto result = backupService.importBackup(from, to);

    assertThat(result.restoredArticleCount()).isEqualTo(0L);
    verify(articleRepositoryCustom, never()).bulkInsertArticle(anyList());
  }

  @Test
  @DisplayName("뉴스 기사 복구 - 이미 DB에 존재하는 URL 복구 스킵")
  void importBackup_SkipExistingUrls() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);
    Instant validInstant = LocalDateTime.of(2026, 4, 15, 12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();

    Resource mockResource = mock(Resource.class);
    InputStream mockInputStream = new ByteArrayInputStream("[]".getBytes());
    given(mockResource.getInputStream()).willReturn(mockInputStream);
    given(articleBackupStorage.loadBackupResources(from, to)).willReturn(List.of(mockResource));

    String duplicateUrl = "http://duplicate.com";
    ArticleBackupDto duplicateDto = ArticleBackupDto.builder()
        .sourceUrl(duplicateUrl)
        .publishDate(validInstant)
        .build();

    given(objectMapper.readValue(any(InputStream.class), ArgumentMatchers.<TypeReference<List<ArticleBackupDto>>>any()))
        .willReturn(List.of(duplicateDto));

    given(articleRepository.findExistingUrls(anyList())).willReturn(Set.of(duplicateUrl));

    backupService.importBackup(from, to);

    verify(articleRepositoryCustom, never()).bulkInsertArticle(anyList());
  }

  @Test
  @DisplayName("뉴스 기사 복구 - DB에 없는 새로운 관심사 생성 후 복구")
  void importBackup_CreateNewInterest() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);
    Instant validInstant = LocalDateTime.of(2026, 4, 15, 12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();

    Resource mockResource = mock(Resource.class);
    InputStream mockInputStream = new ByteArrayInputStream("[]".getBytes());
    given(mockResource.getInputStream()).willReturn(mockInputStream);
    given(articleBackupStorage.loadBackupResources(from, to)).willReturn(List.of(mockResource));

    ArticleBackupDto mockDto = ArticleBackupDto.builder()
        .sourceUrl("http://new-interest.com")
        .publishDate(validInstant)
        .interestKeywords(Map.of("경제", List.of("금리", "물가")))
        .build();

    given(objectMapper.readValue(any(InputStream.class), ArgumentMatchers.<TypeReference<List<ArticleBackupDto>>>any()))
        .willReturn(List.of(mockDto));

    given(articleRepository.findExistingUrls(anyList())).willReturn(Collections.emptySet());

    given(interestRepository.findAll()).willReturn(Collections.emptyList());

    Article mockArticle = Article.builder().title("test").build();
    given(articleBackupMapper.toEntity(mockDto)).willReturn(mockArticle);

    backupService.importBackup(from, to);

    verify(articleRepositoryCustom).bulkInsertArticle(anyList());
    verify(articleRepositoryCustom).bulkInsertArticleInterest(anyList());
  }

  @Test
  @DisplayName("뉴스 기사 복구 - 같은 백업 배치 내에 중복된 URL이 있는 경우")
  void importBackup_SkipIntraBatchDuplicates() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59);
    Instant validInstant = LocalDateTime.of(2026, 4, 15, 12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant();

    Resource mockResource = mock(Resource.class);
    given(mockResource.getInputStream()).willReturn(new ByteArrayInputStream("[]".getBytes()));
    given(articleBackupStorage.loadBackupResources(from, to)).willReturn(List.of(mockResource));

    String sameUrl = "http://same.com";
    ArticleBackupDto dto1 = ArticleBackupDto.builder().sourceUrl(sameUrl).publishDate(validInstant).build();
    ArticleBackupDto dto2 = ArticleBackupDto.builder().sourceUrl(sameUrl).publishDate(validInstant).build();

    given(objectMapper.readValue(any(InputStream.class), ArgumentMatchers.<TypeReference<List<ArticleBackupDto>>>any()))
        .willReturn(List.of(dto1, dto2));

    given(articleRepository.findExistingUrls(anyList())).willReturn(Collections.emptySet());

    Article mockArticle = Article.builder().title("test").build();
    given(articleBackupMapper.toEntity(any())).willReturn(mockArticle);

    backupService.importBackup(from, to);

    verify(articleRepositoryCustom).bulkInsertArticle(articleListCaptor.capture());
    assertThat(articleListCaptor.getValue().size()).isEqualTo(1);
  }
}
