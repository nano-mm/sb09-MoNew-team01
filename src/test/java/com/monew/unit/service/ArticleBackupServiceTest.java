package com.monew.unit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.dto.backup.ArticleBackupDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleInterest;
import com.monew.entity.Interest;
import com.monew.mapper.ArticleBackupMapper;
import com.monew.repository.ArticleInterestRepository;
import com.monew.repository.InterestRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.impl.ArticleBackupServiceImpl;
import com.monew.storage.backup.BackupStorage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArticleBackupServiceTest {

  @InjectMocks
  private ArticleBackupServiceImpl backupService;

  @Mock private ArticleRepository articleRepository;
  @Mock private InterestRepository interestRepository;
  @Mock private ArticleInterestRepository articleInterestRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private ArticleBackupMapper articleBackupMapper;

  @Mock private BackupStorage backupStorage;

  @Test
  @DisplayName("뉴스 기사 백업")
  void export_success() throws Exception {
    Article mockArticle = Article.builder().id(java.util.UUID.randomUUID()).build();
    given(articleRepository.findAll()).willReturn(List.of(mockArticle));
    given(articleInterestRepository.findAllWithInterest()).willReturn(List.of());

    ArticleBackupDto mockDto = ArticleBackupDto.builder()
        .publishDate(Instant.now())
        .build();
    given(articleBackupMapper.toDto(any(), any())).willReturn(mockDto);

    String dummyJson = "[{\"title\":\"test\"}]";
    given(objectMapper.writeValueAsString(any())).willReturn(dummyJson);

    backupService.export();

    verify(backupStorage).saveBackup(anyString(), anyString());
  }

  @Test
  @DisplayName("뉴스 기사 복구")
  void importBackup_success() throws Exception {
    Resource mockResource = mock(Resource.class);
    InputStream mockInputStream = new ByteArrayInputStream("[]".getBytes());
    given(mockResource.getInputStream()).willReturn(mockInputStream);

    given(backupStorage.loadBackupResources()).willReturn(List.of(mockResource));

    ArticleBackupDto mockDto = ArticleBackupDto.builder()
        .title("test")
        .sourceUrl("http://test.com")
        .interestKeywords(Map.of("IT", List.of("인공지능", "출시", "반도체")))
        .build();
    given(objectMapper.readValue(any(InputStream.class), ArgumentMatchers.<TypeReference<List<ArticleBackupDto>>>any()))
        .willReturn(List.of(mockDto));

    given(articleRepository.existsBySourceUrl("http://test.com")).willReturn(false);

    Article mockArticle = Article.builder().title("test").build();
    given(articleBackupMapper.toEntity(mockDto)).willReturn(mockArticle);

    Interest mockInterest = new Interest("IT", List.of("인공지능", "출시", "반도체"));
    given(interestRepository.findAll()).willReturn(List.of(mockInterest));

    backupService.importBackup();

    verify(articleRepository).save(mockArticle);
    verify(articleInterestRepository).save(any(ArticleInterest.class));
  }
}
