package com.monew.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.dto.response.UserActivityDto;
import com.monew.adapter.out.mongo.document.UserActivityDocument;
import com.monew.adapter.out.mongo.repository.UserActivityDocumentRepository;
import com.monew.adapter.out.persistence.SubscriptionRepository;
import com.monew.application.service.UserActivityDtoBuilder;
import com.monew.application.service.UserActivityReadModelService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class UserActivityReadModelServiceTest {

  @Mock
  private ObjectProvider<UserActivityDocumentRepository> documentRepositoryProvider;

  @Mock
  private UserActivityDocumentRepository documentRepository;

  @Mock
  private UserActivityDtoBuilder activityDtoBuilder;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private UserActivityReadModelService userActivityReadModelService;

  @Test
  @DisplayName("Mongo 리포지토리가 없으면 비활성 상태다")
  void isEnabled_False_WhenRepositoryUnavailable() {
    when(documentRepositoryProvider.getIfAvailable()).thenReturn(null);

    assertFalse(userActivityReadModelService.isEnabled());
  }

  @Test
  @DisplayName("Mongo 리포지토리가 있으면 활성 상태다")
  void isEnabled_True_WhenRepositoryAvailable() {
    when(documentRepositoryProvider.getIfAvailable()).thenReturn(documentRepository);

    assertTrue(userActivityReadModelService.isEnabled());
  }

  @Test
  @DisplayName("스냅샷 갱신 시 DTO를 Mongo 문서로 저장한다")
  void refreshSnapshot_SavesDocument() {
    UUID userId = UUID.randomUUID();
    UserActivityDto dto = sampleDto(userId);

    when(documentRepositoryProvider.getIfAvailable()).thenReturn(documentRepository);
    when(activityDtoBuilder.build(userId)).thenReturn(dto);

    userActivityReadModelService.refreshSnapshot(userId);

    verify(documentRepository).save(any(UserActivityDocument.class));
  }

  @Test
  @DisplayName("구독자 스냅샷 일괄 갱신 시 구독자 수만큼 빌더를 호출한다")
  void refreshSnapshotsForInterestSubscribers_RefreshAllSubscribers() {
    UUID interestId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    when(documentRepositoryProvider.getIfAvailable()).thenReturn(documentRepository);
    when(subscriptionRepository.findDistinctUserIdsByInterestId(interestId)).thenReturn(List.of(userId1, userId2));
    when(activityDtoBuilder.build(userId1)).thenReturn(sampleDto(userId1));
    when(activityDtoBuilder.build(userId2)).thenReturn(sampleDto(userId2));

    userActivityReadModelService.refreshSnapshotsForInterestSubscribers(interestId);

    verify(activityDtoBuilder).build(userId1);
    verify(activityDtoBuilder).build(userId2);
    verify(documentRepository, org.mockito.Mockito.times(2)).save(any(UserActivityDocument.class));
  }

  @Test
  @DisplayName("Mongo 리포지토리가 없으면 조회는 빈 결과를 반환한다")
  void findByUserId_Empty_WhenRepositoryUnavailable() {
    UUID userId = UUID.randomUUID();
    when(documentRepositoryProvider.getIfAvailable()).thenReturn(null);

    Optional<UserActivityDto> result = userActivityReadModelService.findByUserId(userId);

    assertTrue(result.isEmpty());
    verify(documentRepository, never()).findById(any());
  }

  @Test
  @DisplayName("Mongo 문서가 있으면 사용자 활동 DTO로 변환해 반환한다")
  void findByUserId_ReturnsDto_WhenDocumentExists() {
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = UserActivityDocument.builder()
        .userId(userId)
        .email("test@test.com")
        .nickname("Tester")
        .userCreatedAt(Instant.now())
        .subscriptions(Collections.emptyList())
        .comments(Collections.emptyList())
        .commentLikes(Collections.emptyList())
        .articleViews(Collections.emptyList())
        .build();

    when(documentRepositoryProvider.getIfAvailable()).thenReturn(documentRepository);
    when(documentRepository.findById(userId)).thenReturn(Optional.of(document));

    Optional<UserActivityDto> result = userActivityReadModelService.findByUserId(userId);

    assertTrue(result.isPresent());
    assertEquals(userId, result.get().id());
    assertEquals("test@test.com", result.get().email());
  }

  private UserActivityDto sampleDto(UUID userId) {
    return UserActivityDto.builder()
        .id(userId)
        .email("test@test.com")
        .nickname("Tester")
        .createdAt(Instant.now())
        .subscriptions(Collections.emptyList())
        .comments(Collections.emptyList())
        .commentLikes(Collections.emptyList())
        .articleViews(Collections.emptyList())
        .build();
  }
}
