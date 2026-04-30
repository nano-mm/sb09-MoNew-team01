package com.monew.service.impl;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestSearchRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.entity.Subscription;
import com.monew.entity.User;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.InterestMapper;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.repository.UserRepository;
import com.monew.service.InterestService;
import com.monew.util.SimilarityUtils;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class InterestServiceImpl implements InterestService {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;

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
  }

  @Override
  public void delete(UUID id) {
    interestRepository.deleteById(id);
  }

  @Override
  public CursorPageResponseDto<InterestDto> find(InterestSearchRequest request) {

    String keyword = request.keyword();
    String orderBy = request.getOrderByOrDefault();
    String direction = request.getDirectionOrDefault();
    String cursor = request.cursorRequest().cursor();
    LocalDateTime after = request.cursorRequest().after();
    int size = request.getSizeOrDefault();
    User user = userRepository.findById(request.userId()).orElseThrow();

    Pageable pageable = PageRequest.of(0, size + 1);

    List<Interest> results;

    boolean isAsc = direction.equalsIgnoreCase("ASC");

    if (orderBy.equals("name")) {

      results = isAsc
          ? interestRepository.findByNameAsc(keyword, cursor, after, pageable)
          : interestRepository.findByNameDesc(keyword, cursor, after, pageable);

    } else if (orderBy.equals("subscriberCount")) {

      Long cursorValue = null;
      try {
        cursorValue = (cursor != null) ? Long.parseLong(cursor) : null;
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("cursor는 숫자여야 합니다.");
      }

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
  public void subscribe(UUID userId, UUID interestId) {

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
  }

  @Override
  public void unsubscribe(UUID userId, UUID interestId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("유저 없음"));

    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new RuntimeException("관심사 없음"));

    Subscription subscription = subscriptionRepository
        .findByUserAndInterest(user, interest)
        .orElseThrow(() -> new RuntimeException("구독 정보 없음"));

    subscriptionRepository.delete(subscription);

    interest.decreaseSubscriber();
  }

}