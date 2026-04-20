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
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  public CommentLike(Comment comment, User user) {
    this.comment = comment;
    this.user = user;
  }

  public Comment getComment() { return comment; }
  public User getUser() { return user; }
  public UUID getCommentId() { return comment.getId(); }
  public UUID getUserId() { return user.getId(); }
}
