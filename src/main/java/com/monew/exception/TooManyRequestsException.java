package com.monew.exception;

public class TooManyRequestsException extends BaseException {
    public TooManyRequestsException() {
        super(ErrorCode.TOO_MANY_REQUESTS);
    }
}
