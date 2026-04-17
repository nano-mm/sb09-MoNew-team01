package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "comment_likes")
@IdClass(CommentLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @Id
  @Column(name = "User", nullable = false)
  private UUID userId;

  public CommentLike(Comment comment, UUID userId) {
    this.comment = comment;
    this.userId = userId;
  }

  public Comment getComment() { return comment; }
  public UUID getUserId() { return userId; }
}