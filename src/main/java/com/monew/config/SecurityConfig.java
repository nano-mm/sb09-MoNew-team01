package com.monew.config;

import com.monew.application.port.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserRepository userRepository;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))

        // 커스텀 필터: Monew-Request-User-ID 헤더를 검증하는 필터
        .addFilterBefore(new PreAuthenticatedUserFilter(userRepository), UsernamePasswordAuthenticationFilter.class)

        .authorizeHttpRequests(auth -> auth
            // 정적 리소스 및 인덱스
            .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/fonts/**").permitAll()

            // Swagger / OpenAPI 관련
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/sb/monew/api/v3/api-docs/**"
            ).permitAll()

            // 사용자 관련
            .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/users/{userId}", "/api/users/{userId}/hard").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/api/users/{userId}").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/user-activities/{userId}").permitAll()

            // 뉴스 기사 관련
            .requestMatchers(HttpMethod.GET, "/api/articles/sources", "/api/articles/restore").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/articles/{articleId}", "/api/articles/{articleId}/hard").permitAll()

            // 관심사 관리 관련
            .requestMatchers(HttpMethod.POST, "/api/interests").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/interests/{interestId}").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/api/interests/{interestId}").permitAll()

            // 댓글 관련
            .requestMatchers(HttpMethod.POST, "/api/comments").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/comments/{commentId}", "/api/comments/{commentId}/hard").permitAll()

            .requestMatchers("/h2-console/**").permitAll()
            .anyRequest().authenticated()
        )

        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
