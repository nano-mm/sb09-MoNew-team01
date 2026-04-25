package com.monew.config;

import com.monew.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class PreAuthenticatedUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private static final String USER_ID_HEADER = "Monew-Request-User-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);

        if (userId != null && !userId.isBlank()) {
            try {
                UUID id = UUID.fromString(userId);
                
                // DB에서 실제 존재하는 유저인지 확인 (Soft Delete 상태도 고려됨)
                userRepository.findById(id).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.getId(), null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
                
            } catch (IllegalArgumentException e) {
                // 잘못된 UUID 형식 무시
            }
        }

        filterChain.doFilter(request, response);
    }
}
