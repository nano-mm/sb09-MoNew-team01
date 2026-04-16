package com.monew.dto.comment;

public enum CommentSortType {
  CREATED_AT,
  LIKE_COUNT;

  public static CommentSortType from(String value) {
    return switch (value.toLowerCase()) {
      case "createdat" -> CREATED_AT;
      case "likecount" -> LIKE_COUNT;
      default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + value);
    };
  }
}
