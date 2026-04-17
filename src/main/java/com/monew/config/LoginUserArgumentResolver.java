package com.monew.config;

import com.monew.entity.User;
import com.monew.exception.user.UnauthorizedException;
import com.monew.exception.user.UserNotFoundException;
import com.monew.repository.UserRepository;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
  private final UserRepository userRepository;

  // A. 어떤 파라미터에 이 리졸버를 적용할지 결정
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    // 1. @LoginUser 어노테이션이 붙어 있고
    // 2. 파라미터 타입이 UUID인 경우에만 true 반환
    return parameter.hasParameterAnnotation(LoginUser.class) &&
           parameter.getParameterType().equals(UUID.class);
  }

  // B. 실제로 어떤 값을 넣어줄지 결정
  @Override
  public Object resolveArgument(@NonNull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) throws BadRequestException {
    String userId = webRequest.getHeader("MoNew-Request-User-ID");

    if (userId == null || userId.isBlank()) {
      throw new UnauthorizedException("인증 헤더가 누락되었습니다.");
    }

    try {
      // 2. 형식 및 DB 존재 여부 검증
      UUID id = UUID.fromString(userId);

      // findById를 통해 실제 유저가 있는지 확인 (SoftDelete 적용 중이라면 삭제된 유저는 조회 안 됨)
      return userRepository.findById(id)
          .map(User::getId)
          .orElseThrow(() -> new UserNotFoundException("유효하지 않은 사용자 ID입니다."));

    } catch (IllegalArgumentException e) {
      throw new BadRequestException("잘못된 UUID 형식입니다.");
    }
  }
}
