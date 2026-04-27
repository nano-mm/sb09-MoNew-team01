package com.monew.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 공통
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류"),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON_409", "이미 존재하는 데이터입니다"),

  // 사용자
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다"),
  EMAIL_DUPLICATION(HttpStatus.CONFLICT, "USER_409", "이메일이 이미 존재합니다"),
  LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_401", "이메일 또는 비밀번호가 틀렸습니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "USER_403", "권한이 없습니다"),

  // 관심사
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "INTEREST_404", "관심사를 찾을 수 없습니다"),
  INTEREST_DUPLICATED(HttpStatus.CONFLICT, "INTEREST_409", "유사한 관심사가 존재합니다"),

  // 기사
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_404", "뉴스 기사를 찾을 수 없습니다"),
  ARTICLE_DUPLICATED(HttpStatus.CONFLICT, "ARTICLE_409", "이미 존재하는 기사입니다"),

  // 댓글
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "댓글을 찾을 수 없습니다"),
  LIKE_DUPLICATED(HttpStatus.CONFLICT, "COMMENT_409", "이미 좋아요를 눌렀습니다"),
  LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "좋아요를 누르지 않은 상태에서는 취소할 수 없습니다."),

  // 알림
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_404", "알림을 찾을 수 없습니다");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
