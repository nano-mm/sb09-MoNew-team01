package com.monew.service;

import com.monew.dto.request.InterestRegisterRequest;
import com.monew.dto.request.InterestUpdateRequest;
import com.monew.dto.response.InterestDto;
import com.monew.entity.Interest;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.InterestMapper;
import com.monew.repository.InterestRepository;
import com.monew.repository.SubscriptionRepository;
import com.monew.util.SimilarityUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class InterestService {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;

  // << 등록 >>
  public Interest create(InterestRegisterRequest request){
    validateDuplicate(request.name());

    Interest interest = new Interest(
        request.name(),
        request.keywords()
    );

    return interestRepository.save(interest);
  }

  // << 유사도 검사 >>
  private void validateDuplicate(String name){
    List<Interest> interests = interestRepository.findAll();

    for(Interest i : interests){
      double similarity = SimilarityUtils.calculateSimilarity(name, i.getName());
      if (similarity>=0.8){
        throw new IllegalArgumentException("이미 유사한 관심사가 존재합니다.");
      }
    }
  }

}