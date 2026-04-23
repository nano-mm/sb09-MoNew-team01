package com.monew.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
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
  protected ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn("Business exception occurred. uri={}, method={}, code={}, message={}", 
        request.getRequestURI(), request.getMethod(), errorCode.getCode(), e.getMessage());

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(ErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    String validationMessage = formatFieldErrors(e.getBindingResult());

    log.warn("Request body validation failed. uri={}, method={}, errors={}", 
        request.getRequestURI(), request.getMethod(), validationMessage);

    String message = validationMessage.isBlank()
        ? ErrorCode.INVALID_INPUT.getMessage()
        : validationMessage;

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
    if ("Wrong email or password".equals(e.getMessage())) {
      log.warn("Authentication failed. uri={}, method={}, reason={}", 
          request.getRequestURI(), request.getMethod(), e.getMessage());
      return ResponseEntity
          .status(ErrorCode.LOGIN_FAILED.getStatus())
          .body(ErrorResponse.of(ErrorCode.LOGIN_FAILED));
    }

    String message = e.getMessage() == null || e.getMessage().isBlank()
        ? ErrorCode.INVALID_INPUT.getMessage()
        : e.getMessage();
    log.warn("Illegal argument occurred. uri={}, method={}, clientMessage={}", 
        request.getRequestURI(), request.getMethod(), message);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
  }

  @ExceptionHandler(NoSuchElementException.class)
  protected ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e, HttpServletRequest request) {
    log.warn("Entity not found. uri={}, method={}, type={}", 
        request.getRequestURI(), request.getMethod(), e.getClass().getSimpleName());

    return ResponseEntity
        .status(ErrorCode.USER_NOT_FOUND.getStatus())
        .body(ErrorResponse.of(ErrorCode.USER_NOT_FOUND));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e, HttpServletRequest request) {
    log.warn("Data integrity violation occurred. uri={}, method={}, message={}", 
        request.getRequestURI(), request.getMethod(), e.getMessage());

    return ResponseEntity
        .status(ErrorCode.DUPLICATE_RESOURCE.getStatus())
        .body(ErrorResponse.of(ErrorCode.DUPLICATE_RESOURCE));
  }

  @ExceptionHandler({
      BindException.class,
      ConstraintViolationException.class,
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class,
      MissingRequestHeaderException.class,
  })
  protected ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
    String message = resolveBadRequestMessage(e);
    log.warn(
        "Bad request exception occurred. uri={}, method={}, type={}, clientMessage={}",
        request.getRequestURI(),
        request.getMethod(),
        e.getClass().getSimpleName(),
        message
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
    log.error("Unhandled exception occurred. uri={}, method={}", 
        request.getRequestURI(), request.getMethod(), e);

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
      String message = formatFieldErrors(bindException.getBindingResult());
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

  private String formatFieldErrors(BindingResult bindingResult) {
    return bindingResult.getFieldErrors()
        .stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .collect(Collectors.joining(", "));
  }
}
