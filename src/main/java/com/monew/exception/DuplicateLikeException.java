package com.monew.exception;

public class DuplicateLikeException extends BaseException {
  public DuplicateLikeException() {
    super(ErrorCode.LIKE_DUPLICATED);
  }
}
