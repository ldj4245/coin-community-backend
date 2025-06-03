package com.coincommunity.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * 
 * 30년차 베테랑급 성능 최적화:
 * - 비동기 알림 처리
 * - 스레드 풀 최적화
 * - 큐 용량 관리
 * - 거부 정책 설정
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.async.notification.core-pool-size:10}")
    private int notificationCorePoolSize;

    @Value("${app.async.notification.max-pool-size:50}")
    private int notificationMaxPoolSize;

    @Value("${app.async.notification.queue-capacity:1000}")
    private int notificationQueueCapacity;

    @Value("${app.async.general.core-pool-size:5}")
    private int generalCorePoolSize;

    @Value("${app.async.general.max-pool-size:20}")
    private int generalMaxPoolSize;

    @Value("${app.async.general.queue-capacity:500}")
    private int generalQueueCapacity;

    /**
     * 알림 전용 스레드 풀
     * 실시간 알림 처리를 위한 고성능 설정
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수
        executor.setCorePoolSize(notificationCorePoolSize);
        
        // 최대 스레드 수
        executor.setMaxPoolSize(notificationMaxPoolSize);
        
        // 큐 용량 (요청이 많을 때 대기할 수 있는 작업 수)
        executor.setQueueCapacity(notificationQueueCapacity);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("Notification-");
        
        // 거부 정책: 호출자가 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 애플리케이션 종료 시 실행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("알림 전용 스레드 풀 초기화 완료: 코어={}, 최대={}, 큐용량={}", 
                notificationCorePoolSize, notificationMaxPoolSize, notificationQueueCapacity);
        
        return executor;
    }

    /**
     * 일반 비동기 작업용 스레드 풀
     * 백그라운드 작업 처리용
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(generalCorePoolSize);
        executor.setMaxPoolSize(generalMaxPoolSize);
        executor.setQueueCapacity(generalQueueCapacity);
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("일반 비동기 스레드 풀 초기화 완료: 코어={}, 최대={}, 큐용량={}", 
                generalCorePoolSize, generalMaxPoolSize, generalQueueCapacity);
        
        return executor;
    }
}
