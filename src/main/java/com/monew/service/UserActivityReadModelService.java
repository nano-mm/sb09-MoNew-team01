package com.monew.service;

import com.monew.dto.response.ArticleViewDto;
import com.monew.dto.response.CommentActivityDto;
import com.monew.dto.response.CommentLikeActivityDto;
import com.monew.dto.response.SubscriptionDto;
import com.monew.dto.response.UserActivityDto;
import com.monew.mongo.document.UserActivityDocument;
import com.monew.mongo.document.UserActivityDocument.ArticleViewEntry;
import com.monew.mongo.document.UserActivityDocument.CommentActivityEntry;
import com.monew.mongo.document.UserActivityDocument.CommentLikeActivityEntry;
import com.monew.mongo.document.UserActivityDocument.SubscriptionEntry;
import com.monew.mongo.repository.UserActivityDocumentRepository;
import com.monew.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityReadModelService {

  private final ObjectProvider<UserActivityDocumentRepository> documentRepository;
  private final UserActivityDtoBuilder activityDtoBuilder;
  private final SubscriptionRepository subscriptionRepository;

  public boolean isEnabled() {
    return documentRepository.getIfAvailable() != null;
  }

  public Optional<UserActivityDto> findByUserId(UUID userId) {
    UserActivityDocumentRepository repo = documentRepository.getIfAvailable();
    if (repo == null) {
      return Optional.empty();
    }

    return repo.findById(userId)
        .map(this::toDto);
  }

  public void refreshSnapshot(UUID userId) {
    UserActivityDocumentRepository repo = documentRepository.getIfAvailable();
    if (repo == null) {
      return;
    }

    UserActivityDto dto = activityDtoBuilder.build(userId);
    repo.save(fromDto(dto));
  }

  public void deleteSnapshot(UUID userId) {
    UserActivityDocumentRepository repo = documentRepository.getIfAvailable();
    if (repo == null) {
      return;
    }

    repo.deleteById(userId);
  }

  public void removeSubscriptionSnapshot(UUID userId, UUID interestId) {
    UserActivityDocumentRepository repo = documentRepository.getIfAvailable();
    if (repo == null) {
      return;
    }

    repo.removeSubscription(userId, interestId);
  }

  public void refreshSnapshotsForInterestSubscribers(UUID interestId) {
    UserActivityDocumentRepository repo = documentRepository.getIfAvailable();
    if (repo == null) {
      return;
    }

    List<UUID> userIds = subscriptionRepository.findDistinctUserIdsByInterestId(interestId);

    for (UUID userId : userIds) {
      refreshSnapshot(userId);
    }
  }

  private UserActivityDto toDto(UserActivityDocument doc) {
    return UserActivityDto.builder()
        .id(doc.getUserId())
        .email(doc.getEmail())
        .nickname(doc.getNickname())
        .createdAt(doc.getUserCreatedAt())
        .subscriptions(doc.getSubscriptions().stream()
            .map(s -> new SubscriptionDto(
                s.getId(),
                s.getInterestId(),
                s.getInterestName(),
                s.getInterestKeywords() != null ? List.copyOf(s.getInterestKeywords()) : List.of(),
                s.getInterestSubscriberCount(),
                s.getCreatedAt()
            ))
            .toList())
        .comments(doc.getComments().stream()
            .map(c -> new CommentActivityDto(
                c.getId(),
                c.getArticleId(),
                c.getArticleTitle(),
                c.getUserId(),
                c.getUserNickname(),
                c.getContent(),
                c.getLikeCount(),
                c.getCreatedAt()
            ))
            .toList())
        .commentLikes(doc.getCommentLikes().stream()
            .map(cl -> new CommentLikeActivityDto(
                cl.getId(),
                cl.getCreatedAt(),
                cl.getCommentId(),
                cl.getArticleId(),
                cl.getArticleTitle(),
                cl.getCommentUserId(),
                cl.getCommentUserNickname(),
                cl.getCommentContent(),
                cl.getCommentLikeCount(),
                cl.getCommentCreatedAt()
            ))
            .toList())
        .articleViews(doc.getArticleViews().stream()
            .map(av -> ArticleViewDto.builder()
                .id(av.getId())
                .viewedBy(av.getViewedBy())
                .createdAt(av.getCreatedAt())
                .articleId(av.getArticleId())
                .source(av.getSource())
                .sourceUrl(av.getSourceUrl())
                .articleTitle(av.getArticleTitle())
                .articlePublishedDate(av.getArticlePublishedDate())
                .articleSummary(av.getArticleSummary())
                .articleCommentCount(av.getArticleCommentCount())
                .articleViewCount(av.getArticleViewCount())
                .build()
            )
            .toList())
        .build();
  }

  private UserActivityDocument fromDto(UserActivityDto dto) {
    return UserActivityDocument.builder()
        .userId(dto.id())
        .email(dto.email())
        .nickname(dto.nickname())
        .userCreatedAt(dto.createdAt())
        .subscriptions(dto.subscriptions().stream()
            .map(s -> SubscriptionEntry.builder()
                .id(s.id())
                .interestId(s.interestId())
                .interestName(s.interestName())
                .interestKeywords(s.interestKeywords() != null ? List.copyOf(s.interestKeywords()) : List.of())
                .interestSubscriberCount(s.interestSubscriberCount())
                .createdAt(s.createdAt())
                .build()
            )
            .toList())
        .comments(dto.comments().stream()
            .map(c -> CommentActivityEntry.builder()
                .id(c.id())
                .articleId(c.articleId())
                .articleTitle(c.articleTitle())
                .userId(c.userId())
                .userNickname(c.userNickname())
                .content(c.content())
                .likeCount(c.likeCount())
                .createdAt(c.createdAt())
                .build()
            )
            .toList())
        .commentLikes(dto.commentLikes().stream()
            .map(cl -> CommentLikeActivityEntry.builder()
                .id(cl.id())
                .createdAt(cl.createdAt())
                .commentId(cl.commentId())
                .articleId(cl.articleId())
                .articleTitle(cl.articleTitle())
                .commentUserId(cl.commentUserId())
                .commentUserNickname(cl.commentUserNickname())
                .commentContent(cl.commentContent())
                .commentLikeCount(cl.commentLikeCount())
                .commentCreatedAt(cl.commentCreatedAt())
                .build()
            )
            .toList())
        .articleViews(dto.articleViews().stream()
            .map(av -> ArticleViewEntry.builder()
                .id(av.id())
                .viewedBy(av.viewedBy())
                .createdAt(av.createdAt())
                .articleId(av.articleId())
                .source(av.source())
                .sourceUrl(av.sourceUrl())
                .articleTitle(av.articleTitle())
                .articlePublishedDate(av.articlePublishedDate())
                .articleSummary(av.articleSummary())
                .articleCommentCount(av.articleCommentCount())
                .articleViewCount(av.articleViewCount())
                .build()
            )
            .toList())
        .build();
  }
}