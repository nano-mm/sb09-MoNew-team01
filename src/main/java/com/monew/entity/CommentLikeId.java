package com.monew.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CommentLikeId implements Serializable {

  private UUID comment;
  private UUID userId;

  protected CommentLikeId() {}

  public CommentLikeId(UUID comment, UUID userId) {
    this.comment = comment;
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CommentLikeId that)) return false;
    return Objects.equals(comment, that.comment)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(comment, userId);
  }
}