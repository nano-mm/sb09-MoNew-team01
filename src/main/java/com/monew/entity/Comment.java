package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.*;
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
@SQLRestriction("deleted_at IS NULL")  // 논리 삭제된 댓글 자동 필터링 (Hibernate 6+)
public class Comment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  // 외부 뉴스 API 기사 ID (문자열로 관리)
  @Column(name = "article_id", nullable = false)
  private String articleId;

  // 작성자 ID (User 도메인 참조 - 직접 FK 대신 ID만 보유)
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "content", nullable = false, length = 1000)
  private String content;

  // 좋아요 수 캐시 (CommentLike 테이블의 카운트를 여기서 관리)
  @Column(name = "like_count", nullable = false)
  private int likeCount = 0;

  // 논리 삭제 필드 - null이면 활성, 값이 있으면 삭제됨
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CommentLike> likes = new ArrayList<>();

  protected Comment() {}

  private Comment(String articleId, UUID userId, String content) {
    this.articleId = articleId;
    this.userId = userId;
    this.content = content;
  }

  public static Comment create(String articleId, UUID userId, String content) {
    return new Comment(articleId, userId, content);
  }

  // 본인 댓글 여부 확인
  public boolean isOwnedBy(UUID requestUserId) {
    return this.userId.equals(requestUserId);
  }

  // 논리 삭제 여부 확인
  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  // 내용 수정
  public void updateContent(String newContent) {
    this.content = newContent;
  }

  // 논리 삭제
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  // 좋아요 수 증가
  public void increaseLikeCount() {
    this.likeCount++;
  }

  // 좋아요 수 감소
  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  public UUID getId() { return id; }
  public String getArticleId() { return articleId; }
  public UUID getUserId() { return userId; }
  public String getContent() { return content; }
  public int getLikeCount() { return likeCount; }
  public LocalDateTime getDeletedAt() { return deletedAt; }
}