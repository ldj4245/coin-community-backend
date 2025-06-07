package com.coincommunity.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API 성능 모니터링 유틸리티
 * API 호출 시간, 성공/실패율 등을 측정하고 기록합니다.
 */
@Slf4j
@Component
public class PerformanceMonitor {
    
    private final Map<String, ApiStats> apiStatsMap = new ConcurrentHashMap<>();
    
    /**
     * API 호출 시작 시간을 기록합니다.
     */
    public long startTimer(String apiName) {
        return System.currentTimeMillis();
    }
    
    /**
     * API 호출 종료 시간을 기록하고 통계를 업데이트합니다.
     */
    public void endTimer(String apiName, long startTime, boolean success) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        ApiStats stats = apiStatsMap.computeIfAbsent(apiName, k -> new ApiStats());
        stats.recordCall(duration, success);
        
        // 느린 API 호출 감지 (5초 이상)
        if (duration > 5000) {
            log.warn("느린 API 호출 감지 - {}: {}ms", apiName, duration);
        }
        
        log.debug("API 호출 완료 - {}: {}ms (성공: {})", apiName, duration, success);
    }
    
    /**
     * 특정 API의 통계 정보를 반환합니다.
     */
    public ApiStats getApiStats(String apiName) {
        return apiStatsMap.get(apiName);
    }
    
    /**
     * 모든 API 통계를 로깅합니다.
     */
    public void logAllStats() {
        log.info("=== API 성능 통계 ===");
        apiStatsMap.forEach((apiName, stats) -> {
            log.info("{}: 평균응답시간={}ms, 성공률={}%, 총호출={}회", 
                apiName, stats.getAverageResponseTime(), stats.getSuccessRate(), stats.getTotalCalls());
        });
        log.info("==================");
    }
    
    /**
     * API 통계 정보를 담는 내부 클래스
     */
    public static class ApiStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        
        public void recordCall(long responseTime, boolean success) {
            totalCalls.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            
            if (success) {
                successfulCalls.incrementAndGet();
            }
            
            // 최대/최소 응답시간 업데이트
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));
            minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
        }
        
        public long getTotalCalls() {
            return totalCalls.get();
        }
        
        public double getSuccessRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) successfulCalls.get() / total * 100 : 0.0;
        }
        
        public long getAverageResponseTime() {
            long total = totalCalls.get();
            return total > 0 ? totalResponseTime.get() / total : 0;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime.get();
        }
        
        public long getMinResponseTime() {
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }
} 