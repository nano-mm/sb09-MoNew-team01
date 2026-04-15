package com.monew.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  protected ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn("Business exception occurred. code={}, message={}", errorCode.getCode(), e.getMessage());

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(ErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e) {
    String validationMessage = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .collect(Collectors.joining(", "));

    log.warn("Request body validation failed. errors={}", validationMessage);

    String message = validationMessage.isBlank()
        ? ErrorCode.INVALID_INPUT.getMessage()
        : validationMessage;

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
  }

  @ExceptionHandler({
      BindException.class,
      ConstraintViolationException.class,
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class,
      MissingRequestHeaderException.class,
      IllegalArgumentException.class
  })
  protected ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
    String message = resolveBadRequestMessage(e);
    log.warn(
        "Bad request exception occurred. type={}, clientMessage={}, detail={}",
        e.getClass().getSimpleName(),
        message,
        e.getMessage()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception occurred", e);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  private String resolveBadRequestMessage(Exception e) {
    if (e instanceof HttpMessageNotReadableException) {
      return "요청 본문 형식이 올바르지 않습니다";
    }

    if (e instanceof MethodArgumentTypeMismatchException) {
      return "요청 파라미터 타입이 올바르지 않습니다";
    }

    if (e instanceof MissingRequestHeaderException) {
      return "필수 요청 헤더가 누락되었습니다";
    }

    if (e instanceof BindException bindException) {
      String message = bindException.getBindingResult()
          .getFieldErrors()
          .stream()
          .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
          .collect(Collectors.joining(", "));
      if (!message.isBlank()) {
        return message;
      }
    }

    if (e instanceof ConstraintViolationException constraintViolationException) {
      String message = constraintViolationException.getConstraintViolations().stream()
          .map(ConstraintViolation::getMessage)
          .collect(Collectors.joining(", "));
      if (!message.isBlank()) {
        return message;
      }
    }

    return ErrorCode.INVALID_INPUT.getMessage();
  }
}
