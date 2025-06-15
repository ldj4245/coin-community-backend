package com.coincommunity.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 성능 모니터링 설정
 * 
 * 주요 기능:
 * - API 응답 시간 측정
 * - 느린 쿼리 감지
 * - 요청 통계 수집
 * - 성능 임계값 알림
 */
@Slf4j
@Configuration
public class PerformanceMonitoringConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceMonitoringInterceptor())
                .addPathPatterns("/api/**", "/users/**", "/posts/**", "/news/**")
                .excludePathPatterns("/health", "/actuator/**", "/swagger-ui/**");
    }

    @Bean
    public HandlerInterceptor performanceMonitoringInterceptor() {
        return new PerformanceMonitoringInterceptor();
    }

    /**
     * API 성능 모니터링 인터셉터
     */
    @Slf4j
    public static class PerformanceMonitoringInterceptor implements HandlerInterceptor {
        
        private static final String START_TIME_ATTRIBUTE = "startTime";
        private static final long SLOW_REQUEST_THRESHOLD_MS = 2000; // 2초
        private static final long VERY_SLOW_REQUEST_THRESHOLD_MS = 5000; // 5초

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute(START_TIME_ATTRIBUTE, startTime);
            
            // 요청 정보 로깅 (DEBUG 레벨)
            log.debug("API 요청 시작: {} {} from {}", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    getClientIpAddress(request));
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                  Object handler, Exception ex) {
            
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime == null) {
                return;
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int statusCode = response.getStatus();
            String clientIp = getClientIpAddress(request);
            
            // 응답 시간에 따른 로그 레벨 조정
            if (duration >= VERY_SLOW_REQUEST_THRESHOLD_MS) {
                log.error("매우 느린 API 응답: {} {} [{}ms] status={} from={}", 
                        method, uri, duration, statusCode, clientIp);
            } else if (duration >= SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("느린 API 응답: {} {} [{}ms] status={} from={}", 
                        method, uri, duration, statusCode, clientIp);
            } else {
                log.info("API 응답 완료: {} {} [{}ms] status={} from={}", 
                        method, uri, duration, statusCode, clientIp);
            }
            
            // 에러 발생 시 추가 로깅
            if (ex != null) {
                log.error("API 처리 중 예외 발생: {} {} [{}ms] error={}", 
                        method, uri, duration, ex.getMessage(), ex);
            }
            
            // 4xx, 5xx 상태 코드 모니터링
            if (statusCode >= 400) {
                log.warn("API 에러 응답: {} {} [{}ms] status={} from={}", 
                        method, uri, duration, statusCode, clientIp);
            }
            
            // 메트릭 수집 (향후 Prometheus/Micrometer 연동 가능)
            collectMetrics(method, uri, duration, statusCode);
        }
        
        /**
         * 실제 클라이언트 IP 주소 획득
         */
        private String getClientIpAddress(HttpServletRequest request) {
            String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP", 
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
            };
            
            for (String header : headers) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.split(",")[0].trim();
                }
            }
            
            return request.getRemoteAddr();
        }
        
        /**
         * 메트릭 수집 (향후 확장)
         */
        private void collectMetrics(String method, String uri, long duration, int statusCode) {
            // TODO: Micrometer 메트릭 수집
            // - 응답 시간 히스토그램
            // - 요청 카운터 (메서드별, URI별, 상태코드별)
            // - 에러율 게이지
            
            // 현재는 로그만 기록
            log.debug("메트릭 수집: method={}, uri={}, duration={}ms, status={}", 
                    method, uri, duration, statusCode);
        }
    }
}
