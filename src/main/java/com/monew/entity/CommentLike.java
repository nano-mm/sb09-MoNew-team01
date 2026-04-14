package com.monew.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_comment", columnNames = {"userId", "commentId"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long commentId;

  public CommentLike(Long userId, Long commentId) {
    this.userId = userId;
    this.commentId = commentId;
  }
}
