package com.monew.exception;

public class ForbiddenException extends BaseException {
  public ForbiddenException() {
    super(ErrorCode.FORBIDDEN);
  }
}
