package com.monew.service.impl;

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
import com.monew.mapper.InterestMapper;
import com.monew.mapper.SubscriptionMapper;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.repository.UserRepository;
import com.monew.service.InterestService;
import com.monew.service.UserActivityReadModelService;
import com.monew.util.SimilarityUtils;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InterestServiceImpl implements InterestService {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final UserActivityReadModelService userActivityReadModelService;
  private final SubscriptionMapper subscriptionMapper;

  @Override
  public Interest create(InterestRegisterRequest request) {
    validateDuplicate(request.name());

    Interest interest = new Interest(
        request.name(),
        request.keywords()
    );

    return interestRepository.save(interest);
  }

  private void validateDuplicate(String name) {
    List<Interest> interests = interestRepository.findAll();

    for (Interest i : interests) {
      double similarity = SimilarityUtils.calculateSimilarity(name, i.getName());
      if (similarity >= 0.8) {
        throw new BaseException(ErrorCode.INTEREST_DUPLICATED);
      }
    }
  }

  @Override
  public void update(UUID id, InterestUpdateRequest request) {
    Interest interest = interestRepository.findById(id)
        .orElseThrow();

    interest.updateKeywords(request.keywords());
    userActivityReadModelService.refreshSnapshotsForInterestSubscribers(id);
  }

  @Override
  public void delete(UUID id) {
    subscriptionRepository.deleteByInterestId(id);
    interestRepository.deleteById(id);
  }

  @Override
  public CursorPageResponseDto<InterestDto> find(String keyword, CursorRequest cursorRequest, UUID userId) {

    String orderBy = cursorRequest.orderBy();
    String direction = cursorRequest.direction();
    String cursor = cursorRequest.cursor();
    Instant after = (cursorRequest.after() != null) ? Instant.from(cursorRequest.after()) : null;
    int size = cursorRequest.limit();
    User user = userRepository.findById(userId).orElseThrow();

    Pageable pageable = PageRequest.of(0, size + 1);

    List<Interest> results;

    boolean isAsc = direction.equalsIgnoreCase("ASC");

    if (orderBy.equals("name")) {

      results = isAsc
          ? interestRepository.findByNameAsc(keyword, cursor, after, pageable)
          : interestRepository.findByNameDesc(keyword, cursor, after, pageable);

    } else if (orderBy.equals("subscriberCount")) {

      Long cursorValue = (cursor != null) ? Long.parseLong(cursor) : null;

      results = isAsc
          ? interestRepository.findBySubscriberAsc(keyword, cursorValue, after, pageable)
          : interestRepository.findBySubscriberDesc(keyword, cursorValue, after, pageable);

    } else {
      throw new IllegalArgumentException("잘못된 orderBy 값");
    }

    boolean hasNext = results.size() > size;

    if (hasNext) {
      results = results.subList(0, size);
    }

    String nextCursor = null;
    Instant nextAfter = null;

    if (!results.isEmpty()) {
      Interest last = results.get(results.size() - 1);

      if (orderBy.equals("name")) {
        nextCursor = last.getName();
      } else{
        nextCursor = String.valueOf(last.getSubscriberCount());
      }

      nextAfter = last.getCreatedAt();
    }

    List<InterestDto> content = results.stream()
        .map(i -> InterestMapper.toDto(i, subscriptionRepository.existsByUserAndInterest(user, i)))
        .toList();

    return CursorPageResponseDto.<InterestDto>builder()
        .content(content)
        .nextCursor(nextCursor)
        .nextAfter(nextAfter)
        .size(size)
        .totalElements(null)
        .hasNext(hasNext)
        .build();
  }

  @Override
  public SubscriptionDto subscribe(UUID userId, UUID interestId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("유저 없음"));

    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new RuntimeException("관심사 없음"));

    if (subscriptionRepository.existsByUserAndInterest(user, interest)) {
      throw new RuntimeException("이미 구독 중입니다.");
    }

    Subscription subscription = new Subscription(user, interest);
    subscriptionRepository.save(subscription);

    interest.increaseSubscriber();
    userActivityReadModelService.refreshSnapshotsForInterestSubscribers(interestId);
    userActivityReadModelService.refreshSnapshot(userId);

    return subscriptionMapper.toDto(subscription);
  }

  @Override
  public void unsubscribe(UUID userId, UUID interestId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("유저 없음"));

    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new RuntimeException("관심사 없음"));

    Optional<Subscription> opt = subscriptionRepository.findByUserAndInterest(user, interest);
    if (opt.isEmpty()) {
      log.debug("구독이 존재하지 않아 무시합니다. userId={}, interestId={}", userId, interestId);
      return;
    }

    Subscription subscription = opt.get();
    try {
      subscriptionRepository.delete(subscription);
    } catch (DataAccessException ex) {
      log.warn("구독 삭제 중 예외 발생(무시). userId={}, interestId={}, error={}", userId, interestId, ex.toString());
    }

    try {
      interest.decreaseSubscriber();
    } catch (Exception ex) {
      log.warn("구독자 수 감소 중 예외 발생(무시). interestId={}, error={}", interestId, ex.toString());
    }


    try {
      userActivityReadModelService.refreshSnapshotsForInterestSubscribers(interestId);
    } catch (Exception ex) {
      log.warn("읽기 모델(구독자) 갱신 실패(무시). interestId={}, error={}", interestId, ex.toString());
    }

    try {
      userActivityReadModelService.removeSubscriptionSnapshot(userId, interestId);
    } catch (Exception ex) {
      log.warn("읽기 모델(사용자) 구독 제거 실패(무시). userId={}, interestId={}, error={}", userId, interestId, ex.toString());
    }
  }

}