package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comment_article_id", columnList = "article_id"),
        @Index(name = "idx_comment_created_at", columnList = "created_at"),
        @Index(name = "idx_comment_like_count", columnList = "like_count")
    }
)
@SQLRestriction("deleted_at IS NULL")
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
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CommentLike> likes = new ArrayList<>();

  private Comment(Article article, User user, String content) {
    this.article = article;
    this.user = user;
    this.content = content;
  }

  protected Comment() {}

  public static Comment create(Article article, User user, String content) {
    return new Comment(article, user, content);
  }

  public boolean isOwnedBy(UUID requestUserId) {
    return this.user.getId().equals(requestUserId);
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  public void updateContent(String newContent) {
    this.content = newContent;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
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

  public UUID getUserId() {
    return user.getId();
  }
  public String getContent() { return content; }
  public int getLikeCount() { return likeCount; }
}