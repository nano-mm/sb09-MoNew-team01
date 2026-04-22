package com.monew.config;

import com.monew.repository.UserRepository;
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
        // 1. CSRF 비활성화
        .csrf(AbstractHttpConfigurer::disable)

        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin)
        )

        // 2. 커스텀 필터 추가: 헤더 기반 인증 처리 (DB 검증 포함)
        .addFilterBefore(new PreAuthenticatedUserFilter(userRepository), UsernamePasswordAuthenticationFilter.class)

        // 3. 경로별 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/assets/**", "/actuator/**").permitAll() // 메인 및 정적 리소스
            .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll() // 회원가입 및 로그인
            .requestMatchers("/h2-console/**").permitAll() // H2 콘솔
            .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        )

        // 4. 나머지 기본 보안 기능 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable);

    return http.build();
  }
}