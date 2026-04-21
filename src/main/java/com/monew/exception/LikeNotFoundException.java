package com.monew.exception;

public class LikeNotFoundException extends BaseException {

  public LikeNotFoundException() {
    super(ErrorCode.LIKE_NOT_FOUND);
  }
}