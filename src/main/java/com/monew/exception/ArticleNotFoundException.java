package com.monew.exception;

public class ArticleNotFoundException extends BaseException {

  public ArticleNotFoundException() {
    super(ErrorCode.ARTICLE_NOT_FOUND);
  }

  public ArticleNotFoundException(String id) {
    super(ErrorCode.ARTICLE_NOT_FOUND);
  }
}