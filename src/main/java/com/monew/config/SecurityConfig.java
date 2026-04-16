package com.monew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. CSRF 비활성화: REST API에서는 보통 끕니다. (이게 안 되어 있으면 403이 납니다)
        .csrf(csrf -> csrf.disable())

        // 2. 경로별 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/assets/**","/api/**").permitAll() // 테스트를 위해 우선 모두 허용
            .anyRequest().authenticated()
        )

        // 3. 나머지 기본 보안 기능 비활성화 (필요 시)
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable());

    return http.build();
  }
}