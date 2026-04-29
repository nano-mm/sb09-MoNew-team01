package com.monew.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID = "requestId";
  private static final String REQUEST_IP = "requestIp";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    String requestIp = request.getRemoteAddr();

    MDC.put(REQUEST_ID, requestId);
    MDC.put(REQUEST_IP, requestIp);

    response.setHeader("X-Request-ID", requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // 요청 처리가 끝나면 무조건 MDC 비우기
      MDC.clear();
    }
  }
}
