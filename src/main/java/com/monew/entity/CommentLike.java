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
  @Column(name = "comment_id", nullable = false)
  private UUID commentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", insertable = false, updatable = false)
  private Comment comment;

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  public CommentLike(Comment comment, User user) {
    this.commentId = comment.getId();  // UUID 값 직접 세팅
    this.comment = comment;
    this.userId = user.getId();        // UUID 값 직접 세팅
    this.user = user;
  }

  public Comment getComment() { return comment; }
  public User getUser() { return user; }
  public UUID getCommentId() { return commentId; }
  public UUID getUserId() { return userId; }
}
