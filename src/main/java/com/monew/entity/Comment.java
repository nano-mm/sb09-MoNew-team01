package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comment_article_id", columnList = "article_id"),
        @Index(name = "idx_comment_created_at", columnList = "created_at"),
        @Index(name = "idx_comment_like_count", columnList = "like_count")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "content", nullable = false, length = 1000)
  private String content;

  @Column(name = "like_count", nullable = false)
  private int likeCount = 0;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CommentLike> likes = new ArrayList<>();

  private Comment(Article article, User user, String content) {
    this.article = article;
    this.user = user;
    this.content = content;
  }

  public static Comment create(Article article, User user, String content) {
    return new Comment(article, user, content);
  }

  public boolean isOwnedBy(UUID requestUserId) {
    return this.user.getId().equals(requestUserId);
  }

  public void updateContent(String newContent) {
    this.content = newContent;
  }

  public void softDelete(Instant time) {
    this.deletedAt = time;
  }

  public void increaseLikeCount() {
    this.likeCount++;
  }

  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  public UUID getArticleId() {
    return article.getId();
  }

  public String getArticleTitle() {
    return article.getTitle();
  }

  public UUID getUserId() {
    return user.getId();
  }

  public String getUserNickname() {
    return user.getNickname();
  }
}
