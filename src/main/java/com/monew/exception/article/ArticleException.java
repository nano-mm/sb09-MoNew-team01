package com.monew.exception.article;

import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;

public abstract class ArticleException extends BaseException {

  public ArticleException(ErrorCode errorCode) {
    super(errorCode);
  }
}
