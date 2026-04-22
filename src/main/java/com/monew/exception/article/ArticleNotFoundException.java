package com.monew.exception.article;

import com.monew.exception.ErrorCode;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

  public ArticleNotFoundException(UUID articleId) {

    super(ErrorCode.ARTICLE_NOT_FOUND);
  }
}
