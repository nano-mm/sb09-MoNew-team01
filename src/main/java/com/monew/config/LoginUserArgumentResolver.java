package com.monew.config;

import com.monew.exception.user.UnauthorizedException;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(LoginUser.class) &&
           parameter.getParameterType().equals(UUID.class);
  }

  @Override
  public Object resolveArgument(@NonNull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    
    // SecurityContext에서 Filter가 저장해둔 인증 정보 가져오기
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
      throw new UnauthorizedException("인증 정보가 없습니다.");
    }

    // Filter에서 Principal에 UUID를 넣어두었으므로 이를 반환
    Object principal = authentication.getPrincipal();
    if (principal instanceof UUID) {
      return principal;
    }

    throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
  }
}
