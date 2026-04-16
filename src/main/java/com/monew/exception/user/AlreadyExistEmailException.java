package com.monew.exception.user;

import org.springframework.dao.DataIntegrityViolationException;

public class AlreadyExistEmailException extends DataIntegrityViolationException {

  public AlreadyExistEmailException(String message) {
    super(message);
  }
}
