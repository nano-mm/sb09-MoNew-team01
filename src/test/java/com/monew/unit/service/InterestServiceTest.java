package com.monew.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.monew.dto.request.CursorRequest;
import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.dto.response.SubscriptionDto;
import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import com.monew.entity.User;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.SubscriptionMapper;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.repository.UserRepository;

import com.monew.service.InterestService;
import com.monew.service.UserActivityReadModelService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock private InterestRepository interestRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserActivityReadModelService userActivityReadModelService;
  @Mock private SubscriptionMapper subscriptionMapper;
  @Mock private EntityManager entityManager;

  @InjectMocks
  private InterestService interestService;

  private UUID userId;
  private UUID interestId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    interestId = UUID.randomUUID();

    interestService = new InterestService(
        interestRepository,
        subscriptionRepository,
        userRepository,
        userActivityReadModelService,
        subscriptionMapper,
        entityManager
    );
  }

  // 생성 성공
  @Test
  @DisplayName("관심사 생성 성공")
  void create_Success() {
    InterestRegisterRequest request =
        new InterestRegisterRequest("스포츠", List.of("축구"));

    given(interestRepository.findAll()).willReturn(List.of());
    given(interestRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    Interest result = interestService.create(request);

    assertThat(result.getName()).isEqualTo("스포츠");
  }

  // 생성 실패
  @Test
  @DisplayName("관심사 생성 실패 - 유사도 중복")
  void create_Fail_Duplicate() {
    InterestRegisterRequest request =
        new InterestRegisterRequest("스포츠", List.of("축구"));

    Interest existing = new Interest("스포츠", List.of("야구"));

    given(interestRepository.findAll()).willReturn(List.of(existing));

    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(BaseException.class)
        .hasMessageContaining(ErrorCode.INTEREST_DUPLICATED.getMessage());

  }

  // 수정 성공
  @Test
  @DisplayName("관심사 수정 성공 (키워드만 변경)")
  void update_Success() {
    Interest interest = new Interest("스포츠", List.of("축구"));

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    interestService.update(
        interestId,
        new InterestUpdateRequest(List.of("야구"))
    );

    assertThat(interest.getKeywords()).contains("야구");
    verify(userActivityReadModelService).refreshSnapshotsForInterestSubscribers(interestId);

  }

  // 삭제 성공
  @Test
  @DisplayName("관심사 삭제 성공")
  void delete_Success() {
    interestService.delete(interestId);

    verify(interestRepository).deleteById(interestId);
    verify(subscriptionRepository).deleteByInterestId(interestId);
  }

  // 목록 조회 성공
  @Test
  @DisplayName("관심사 목록 조회 성공")
  void find_Success() {

    User user = mock(User.class);
    Interest interest = new Interest("스포츠", List.of("축구"));

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByNameAsc(any(), any(), any(), any()))
        .willReturn(List.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(any(), any()))
        .willReturn(false);

    CursorRequest cursorRequest =
        new CursorRequest(null, null, 10, "name", "ASC");

    CursorPageResponseDto<InterestDto> result =
        interestService.find(null, cursorRequest, userId);

    assertThat(result.content()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
  }

  // 구독 성공
  @Test
  @DisplayName("구독 성공")
  void subscribe_Success() {
    User user = mock(User.class);
    Instant now = Instant.now();
    Subscription subscription = mock(Subscription.class);
    Interest interest = new Interest("스포츠", List.of("축구"));

    SubscriptionDto responseDto = new SubscriptionDto(
        subscription.getId(),
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        now
    );

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByIdWithPessimisticLock(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(user, interest))
        .willReturn(false);

    given(subscriptionRepository.saveAndFlush(any()))
        .willReturn(subscription);

    given(subscriptionMapper.toDto(any(Subscription.class)))
        .willReturn(responseDto);

    SubscriptionDto result = interestService.subscribe(userId, interestId);

    assertThat(result).isNotNull();
    assertThat(result.interestName()).isEqualTo("스포츠");
    verify(interestRepository).updateSubscriberCount(interestId);
    verify(entityManager).refresh(interest);
  }

  //구독 실패
  @Test
  @DisplayName("구독 실패 - 이미 구독")
  void subscribe_Fail_Already() {
    User user = mock(User.class);
    Interest interest = new Interest("스포츠", List.of("축구"));

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByIdWithPessimisticLock(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(user, interest))
        .willReturn(true);

    assertThatThrownBy(() ->
        interestService.subscribe(userId, interestId))
        .isInstanceOf(BaseException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
  }

  // 구독 취소 성공
  @Test
  @DisplayName("구독 취소 성공")
  void unsubscribe_Success() {
    User user = mock(User.class);
    Interest interest = mock(Interest.class);
    Subscription subscription = mock(Subscription.class);

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByIdWithPessimisticLock(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.findAllByUserAndInterest(any(), any()))
        .willReturn(List.of(subscription));

    interestService.unsubscribe(userId, interestId);

    verify(subscriptionRepository).delete(subscription);
    verify(interestRepository).updateSubscriberCount(interestId);
    verify(entityManager).refresh(interest);
  }

  //구독 취소 실패
  @Test
  @DisplayName("구독 취소 실패 - 구독 정보 없음")
  void unsubscribe_Fail_NotFound() {
    User user = mock(User.class);
    Interest interest = mock(Interest.class);

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByIdWithPessimisticLock(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.findAllByUserAndInterest(user, interest))
        .willReturn(List.of());

    assertThatThrownBy(() -> interestService.unsubscribe(userId, interestId))
        .isInstanceOf(BaseException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBSCRIPTION_NOT_FOUND);
  }

  @Test
  @DisplayName("구독 시 DB 제약조건 위반 시 에러 발생")
  void subscribe_DataIntegrityViolation_ThrowsException() {
    User user = mock(User.class);
    Interest interest = new Interest("스포츠", List.of());

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findByIdWithPessimisticLock(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserAndInterest(user, interest)).willReturn(false);

    given(subscriptionRepository.saveAndFlush(any()))
        .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> interestService.subscribe(userId, interestId))
        .isInstanceOf(BaseException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
  }
}
