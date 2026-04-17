package com.monew.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CommentLikeId implements Serializable {

  private UUID comment;
  private UUID user;

  protected CommentLikeId() {}

  public CommentLikeId(UUID comment, UUID user) {
    this.comment = comment;
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CommentLikeId that)) return false;
    return Objects.equals(comment, that.comment)
        && Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(comment, user);
  }
}