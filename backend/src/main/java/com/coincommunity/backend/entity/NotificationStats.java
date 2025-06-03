package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 알림 전송 통계 엔티티
 */
@Entity
@Table(name = "notification_stats")
@Getter
@Setter
@NoArgsConstructor
public class NotificationStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 통계 날짜 (YYYY-MM-DD)
    @Column(name = "stat_date", nullable = false)
    private String statDate;

    // 알림 타입 (price, community, news, analysis, market)
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    // 전송 방식 (push, web, both)
    @Column(name = "delivery_method", nullable = false, length = 20)
    private String deliveryMethod;

    // 총 전송 시도 횟수
    @Column(name = "total_attempts", nullable = false)
    private Long totalAttempts = 0L;

    // 성공한 전송 횟수
    @Column(name = "successful_deliveries", nullable = false)
    private Long successfulDeliveries = 0L;

    // 실패한 전송 횟수
    @Column(name = "failed_deliveries", nullable = false)
    private Long failedDeliveries = 0L;

    // 수신자 수
    @Column(name = "unique_recipients", nullable = false)
    private Long uniqueRecipients = 0L;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 생성자
    public NotificationStats(String statDate, String notificationType, String deliveryMethod) {
        this.statDate = statDate;
        this.notificationType = notificationType;
        this.deliveryMethod = deliveryMethod;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 전송 시도 횟수 증가
     */
    public void incrementAttempts() {
        this.totalAttempts++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 성공한 전송 횟수 증가
     */
    public void incrementSuccessful() {
        this.successfulDeliveries++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 실패한 전송 횟수 증가
     */
    public void incrementFailed() {
        this.failedDeliveries++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수신자 수 증가
     */
    public void incrementRecipients() {
        this.uniqueRecipients++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        if (totalAttempts == 0) {
            return 0.0;
        }
        return (double) successfulDeliveries / totalAttempts * 100.0;
    }

    /**
     * 실패율 계산
     */
    public double getFailureRate() {
        if (totalAttempts == 0) {
            return 0.0;
        }
        return (double) failedDeliveries / totalAttempts * 100.0;
    }
}
