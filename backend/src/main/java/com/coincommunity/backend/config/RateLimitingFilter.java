package com.coincommunity.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Rate Limiting 필터 (개선된 버전)
 * 
 * 주요 기능:
 * - IP 기반 요청 제한
 * - 사용자별 요청 제한
 * - 엔드포인트별 세분화된 제한
 * - 동적 제한 설정
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Qualifier("stringRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;
    
    // 생성자 주입으로 RedisTemplate 의존성 주입
    public RateLimitingFilter(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    // Rate limiting 설정
    private static final int DEFAULT_LIMIT_PER_MINUTE = 60;  // 기본: 분당 60회
    private static final int API_LIMIT_PER_MINUTE = 100;     // API: 분당 100회
    private static final int AUTH_LIMIT_PER_MINUTE = 10;     // 인증: 분당 10회
    private static final int SEARCH_LIMIT_PER_MINUTE = 30;   // 검색: 분당 30회
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = getClientIdentifier(request);
        String endpoint = getEndpointCategory(request);
        int limit = getLimitForEndpoint(endpoint);
        
        if (!isRequestAllowed(clientId, endpoint, limit)) {
            log.warn("Rate limit exceeded: clientId={}, endpoint={}, limit={}", 
                    clientId, endpoint, limit);
            
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"요청 한도가 초과되었습니다. 잠시 후 다시 시도해주세요.\",\"data\":null}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 클라이언트 식별자 생성 (IP + User Agent)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    /**
     * 실제 클라이언트 IP 주소 획득
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 첫 번째 IP가 실제 클라이언트 IP
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 엔드포인트 카테고리 분류
     */
    private String getEndpointCategory(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        if (uri.startsWith("/users/login") || uri.startsWith("/users/register")) {
            return "auth";
        } else if (uri.contains("/search")) {
            return "search";
        } else if (uri.startsWith("/api/")) {
            return "api";
        } else {
            return "default";
        }
    }
    
    /**
     * 엔드포인트별 제한 수 반환
     */
    private int getLimitForEndpoint(String endpoint) {
        return switch (endpoint) {
            case "auth" -> AUTH_LIMIT_PER_MINUTE;
            case "search" -> SEARCH_LIMIT_PER_MINUTE;
            case "api" -> API_LIMIT_PER_MINUTE;
            default -> DEFAULT_LIMIT_PER_MINUTE;
        };
    }
    
    /**
     * 요청 허용 여부 확인 (Sliding Window 방식)
     */
    private boolean isRequestAllowed(String clientId, String endpoint, int limit) {
        String key = "rate_limit:" + endpoint + ":" + clientId;
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - TimeUnit.MINUTES.toMillis(1);
        
        try {
            // 현재 시간을 기록
            redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
            
            // 1분 이전 데이터 제거
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            
            // 현재 요청 수 확인
            Long requestCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
            
            // TTL 설정 (메모리 최적화)
            redisTemplate.expire(key, Duration.ofMinutes(2));
            
            return requestCount <= limit;
            
        } catch (Exception e) {
            log.error("Rate limiting 검사 중 오류 발생: key={}", key, e);
            // Redis 오류 시 요청 허용 (fail-open)
            return true;
        }
    }
    
    /**
     * 특정 URL은 Rate Limiting 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/health") || 
               uri.startsWith("/actuator") || 
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs");
    }
}
