package com.monew.application.service;

import com.monew.dto.response.UserActivityDto;
import com.monew.domain.model.User;
import com.monew.mapper.ArticleViewMapper;
import com.monew.mapper.CommentMapper;
import com.monew.mapper.SubscriptionMapper;
import com.monew.adapter.out.persistence.ArticleViewRepository;
import com.monew.adapter.out.persistence.CommentLikeRepository;
import com.monew.adapter.out.persistence.CommentRepository;
import com.monew.adapter.out.persistence.SubscriptionRepository;
import com.monew.adapter.out.persistence.UserRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserActivityDtoBuilder {

  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleViewRepository articleViewRepository;
  private final SubscriptionMapper subscriptionMapper;
  private final CommentMapper commentMapper;
  private final ArticleViewMapper articleViewMapper;

  @Transactional(readOnly = true)
  public UserActivityDto build(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

    return UserActivityDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .createdAt(user.getCreatedAt())
        .subscriptions(subscriptionRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(subscriptionMapper::toDto)
            .toList())
        .comments(commentRepository.findTop10ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
            .map(commentMapper::toActivityDto)
            .toList())
        .commentLikes(commentLikeRepository.findTop10ByUser_IdAndComment_DeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
            .map(commentMapper::toLikeActivityDto)
            .toList())
        .articleViews(articleViewRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(av -> articleViewMapper.toDto(av, av.getArticle()))
            .toList())
        .build();
  }
}
