package com.monew.exception;

public class CommentNotFoundException extends BaseException {

  public CommentNotFoundException() {
    super(ErrorCode.COMMENT_NOT_FOUND);
  }

}

