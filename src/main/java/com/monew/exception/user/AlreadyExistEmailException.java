package com.monew.exception.user;

import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;

public class AlreadyExistEmailException extends BaseException {

  public AlreadyExistEmailException() {
    super(ErrorCode.EMAIL_DUPLICATION);
  }
}
