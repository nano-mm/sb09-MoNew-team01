package com.monew.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import com.monew.entity.User;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.repository.UserRepository;
import com.monew.service.impl.InterestServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.mockito.*;

class InterestServiceTest {

  @Mock private InterestRepository interestRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks
  private InterestServiceImpl interestService;

  private UUID userId;
  private UUID interestId;
  private User user;
  private Interest interest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    userId = UUID.randomUUID();
    interestId = UUID.randomUUID();

    user = User.builder()
        .email("test@test.com")
        .nickname("tester")
        .password("1234")
        .build();

    interest = new Interest("스포츠", List.of("축구"));
  }

  // 생성 성공
  @Test
  @DisplayName("관심사 생성 성공")
  void create_Success() {
    InterestRegisterRequest request =
        new InterestRegisterRequest("스포츠", List.of("축구"));

    given(interestRepository.findAll()).willReturn(List.of());
    given(interestRepository.save(any())).willReturn(interest);

    Interest result = interestService.create(request);

    assertThat(result).isNotNull();
    verify(interestRepository).save(any());
  }

  // 생성 실패
  @Test
  @DisplayName("관심사 생성 실패 - 유사도 중복")
  void create_Fail_Duplicate() {
    InterestRegisterRequest request =
        new InterestRegisterRequest("스포츠", List.of("축구"));

    given(interestRepository.findAll()).willReturn(List.of(
        new Interest("스포츠", List.of())
    ));

    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(RuntimeException.class);
  }

  // 수정 성공
  @Test
  @DisplayName("관심사 수정 성공 (키워드만 변경)")
  void update_Success() {
    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    interestService.update(
        interestId,
        new InterestUpdateRequest(List.of("야구"))
    );

    assertThat(interest.getKeywords()).contains("야구");
  }

  // 삭제 성공
  @Test
  @DisplayName("관심사 삭제 성공")
  void delete_Success() {
    interestService.delete(interestId);

    verify(interestRepository).deleteById(interestId);
  }

  // 목록 조회 성공
  @Test
  @DisplayName("관심사 목록 조회 성공")
  void find_Success() {

    InterestSearchRequest request = new InterestSearchRequest(
        "스포츠",
        "name",
        "ASC",
        null,
        null,
        10,
        userId
    );

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findByNameAsc(any(), any(), any(), any()))
        .willReturn(List.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(any(), any()))
        .willReturn(false);

    CursorPageResponseDto<InterestDto> result =
        interestService.find(request);

    assertThat(result.content()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
  }

  // 구독 성공
  @Test
  @DisplayName("구독 성공")
  void subscribe_Success() {
    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(user, interest))
        .willReturn(false);

    interestService.subscribe(userId, interestId);

    verify(subscriptionRepository).save(any(Subscription.class));
  }

  //구독 실패
  @Test
  @DisplayName("구독 실패 - 이미 구독")
  void subscribe_Fail_Already() {
    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.existsByUserAndInterest(user, interest))
        .willReturn(true);

    assertThatThrownBy(() ->
        interestService.subscribe(userId, interestId))
        .isInstanceOf(RuntimeException.class);
  }

  // 구독 취소 성공
  @Test
  @DisplayName("구독 취소 성공")
  void unsubscribe_Success() {
    Subscription subscription = new Subscription(user, interest);

    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.findByUserAndInterest(user, interest))
        .willReturn(Optional.of(subscription));

    interestService.unsubscribe(userId, interestId);

    verify(subscriptionRepository).delete(subscription);
  }

  //구독 취소 실패
  @Test
  @DisplayName("구독 취소 실패 - 구독 없음")
  void unsubscribe_Fail() {
    given(userRepository.findById(userId))
        .willReturn(Optional.of(user));

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    given(subscriptionRepository.findByUserAndInterest(user, interest))
        .willReturn(Optional.empty());

    assertThatThrownBy(() ->
        interestService.unsubscribe(userId, interestId))
        .isInstanceOf(RuntimeException.class);
  }
}