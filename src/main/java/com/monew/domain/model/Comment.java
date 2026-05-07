package com.monew.domain.model;

import com.monew.domain.model.base.BaseEntity;
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
  @JoinColumn(name = "article_id", insertable = false, updatable = false)
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  // ✅ FK를 직접 컬럼으로 보유 → 세션 없이 안전하게 접근 가능
  @Column(name = "article_id", nullable = false)
  private UUID articleId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  // article title은 별도 저장 (비정규화) 또는 article fetch 시점에만 사용
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
    this.articleId = article.getId();
    this.user = user;
    this.userId = user.getId();
    this.content = content;
  }

  public static Comment create(Article article, User user, String content) {
    return new Comment(article, user, content);
  }

  public boolean isOwnedBy(UUID requestUserId) {
    return this.userId.equals(requestUserId);
  }

  public UUID getArticleId() {
    return this.articleId;
  }

  public String getArticleTitle() {
    // article proxy 접근은 필요할 때 명시적으로 (서비스 레이어에서 트랜잭션 내)
    return article.getTitle();
  }

  public UUID getUserId() {
    return this.userId;
  }

  public String getUserNickname() {
    return user.getNickname();  // 필요하면 nickname도 컬럼 추가 고려
  }

  public void updateContent(String newContent) { this.content = newContent; }
  public void softDelete(Instant time) { this.deletedAt = time; }
  public void increaseLikeCount() { this.likeCount++; }
  public void decreaseLikeCount() {
    if (this.likeCount > 0) this.likeCount--;
  }
}
