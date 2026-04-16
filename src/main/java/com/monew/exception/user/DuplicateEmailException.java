package com.monew.exception.user;

public class DuplicateEmailException extends UserException {

  public DuplicateEmailException(String message) {
    super(message);
  }
}
