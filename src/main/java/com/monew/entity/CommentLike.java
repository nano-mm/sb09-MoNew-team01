package com.monew.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_likes")
@IdClass(CommentLikeId.class)
@EntityListeners(AuditingEntityListener.class)
public class CommentLike {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @Id
  @Column(name = "User", nullable = false)
  private UUID userId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected CommentLike() {}

  public CommentLike(Comment comment, UUID userId) {
    this.comment = comment;
    this.userId = userId;
  }

  public Comment getComment() { return comment; }
  public UUID getUserId() { return userId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}