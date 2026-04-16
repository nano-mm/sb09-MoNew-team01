package com.monew.exception;

public class DuplicateLikeException extends RuntimeException {
  public DuplicateLikeException() {
    super("이미 좋아요를 눌렀습니다.");
  }
}
