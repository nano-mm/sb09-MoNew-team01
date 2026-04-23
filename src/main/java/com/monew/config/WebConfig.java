package com.monew.config;

import com.monew.dto.comment.CommentSortType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final LoginUserArgumentResolver loginUserArgumentResolver;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    // 스프링이 사용할 리졸버 목록에 우리가 만든 것을 추가합니다.
    resolvers.add(loginUserArgumentResolver);
  }


  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, CommentSortType.class, CommentSortType::from);
  }

}
