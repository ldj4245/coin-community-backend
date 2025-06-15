package com.coincommunity.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 토큰을 검증하고 인증 정보를 설정하는 개선된 필터
 * 
 * 주요 개선사항:
 * - 스킵할 URL 패턴 정의로 성능 최적화
 * - 향상된 로깅 및 예외 처리
 * - API 요청 감지 및 적절한 응답 처리
 * - 토큰 검증 강화
 */
@Slf4j
@Component("jwtAuthenticationFilterImproved")
@RequiredArgsConstructor
public class JwtAuthenticationFilterImproved extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    
    // JWT 인증을 스킵할 URL 패턴들 (성능 최적화)
    private static final List<String> SKIP_FILTER_URLS = Arrays.asList(
        "/users/register", "/users/login", "/users/refresh",
        "/trending-coins", "/coins", "/news", "/exchange-prices", "/kimchi-premium",
        "/posts", "/health", "/actuator", "/swagger-ui", "/v3/api-docs", "/ws"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return SKIP_FILTER_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 요청에서 JWT 토큰 추출
            String token = resolveToken(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 토큰이 유효한 경우 인증 정보 설정
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT 인증 성공: 사용자={}, URI={}", 
                         authentication.getName(), request.getRequestURI());
            } else if (token != null) {
                // 토큰이 있지만 유효하지 않은 경우
                log.debug("유효하지 않은 JWT 토큰: URI={}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류가 발생했습니다: URI={}", request.getRequestURI(), e);
            // 인증 정보 제거
            SecurityContextHolder.clearContext();
            
            // API 요청의 경우 JSON 에러 응답 전송
            if (isApiRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"인증 토큰이 유효하지 않습니다.\",\"data\":null}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 요청 헤더에서 JWT 토큰 추출 (개선된 추출 로직)
     */
    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            
            // 토큰 길이 유효성 검사 (기본적인 검증)
            if (token.length() < 10) {
                log.debug("토큰이 너무 짧습니다: length={}", token.length());
                return null;
            }
            
            return token;
        }
        
        return null;
    }
    
    /**
     * API 요청인지 확인 (JSON 응답이 필요한 요청)
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");
        
        return uri.startsWith("/api/") || 
               (contentType != null && contentType.contains("application/json")) ||
               (accept != null && accept.contains("application/json"));
    }
}
