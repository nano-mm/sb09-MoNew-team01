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
  @JoinColumn(name = "comment_id", nullable = false, insertable = false, updatable = false)
  private Comment comment;

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
  private User user;

  public CommentLike(Comment comment, User user) {
    this.comment = comment;
    this.commentId = comment.getId();
    this.user = user;
    this.userId = user.getId();
  }

  public Comment getComment() { return comment; }
  public User getUser() { return user; }
}