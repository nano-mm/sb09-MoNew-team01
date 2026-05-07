package com.monew.adapter.out.mongo.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityDocument {

  @Id
  private UUID userId;

  private String email;
  private String nickname;
  private Instant userCreatedAt;

  @Builder.Default
  private List<SubscriptionEntry> subscriptions = new ArrayList<>();

  @Builder.Default
  private List<CommentActivityEntry> comments = new ArrayList<>();

  @Builder.Default
  private List<CommentLikeActivityEntry> commentLikes = new ArrayList<>();

  @Builder.Default
  private List<ArticleViewEntry> articleViews = new ArrayList<>();

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SubscriptionEntry {
    private UUID id;
    private UUID interestId;
    private String interestName;
    private List<String> interestKeywords;
    private Long interestSubscriberCount;
    private Instant createdAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CommentActivityEntry {
    private UUID id;
    private UUID articleId;
    private String articleTitle;
    private UUID userId;
    private String userNickname;
    private String content;
    private long likeCount;
    private Instant createdAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CommentLikeActivityEntry {
    private UUID id;
    private Instant createdAt;
    private UUID commentId;
    private UUID articleId;
    private String articleTitle;
    private UUID commentUserId;
    private String commentUserNickname;
    private String commentContent;
    private long commentLikeCount;
    private Instant commentCreatedAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ArticleViewEntry {
    private UUID id;
    private UUID viewedBy;
    private Instant createdAt;
    private UUID articleId;
    private String source;
    private String sourceUrl;
    private String articleTitle;
    private Instant articlePublishedDate;
    private String articleSummary;
    private Long articleCommentCount;
    private Long articleViewCount;
  }
}
