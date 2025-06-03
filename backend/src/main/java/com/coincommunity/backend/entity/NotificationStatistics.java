package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 통계 엔티티
 * 알림 전송 성공/실패 통계를 관리합니다.
 */
@Entity
@Table(name = "notification_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatistics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_statistics_id")
    private Long id;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "delivery_method", nullable = false, length = 20)
    private String deliveryMethod; // WEBSOCKET, FCM, EMAIL

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Long successCount = 0L;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Long failureCount = 0L;

    @Column(name = "total_count", nullable = false)
    @Builder.Default
    private Long totalCount = 0L;

    @Column(name = "date_hour", nullable = false)
    private LocalDateTime dateHour; // 시간별 통계를 위한 필드

    /**
     * 성공 카운트 증가
     */
    public void incrementSuccess() {
        this.successCount++;
        this.totalCount++;
    }

    /**
     * 실패 카운트 증가
     */
    public void incrementFailure() {
        this.failureCount++;
        this.totalCount++;
    }

    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }

    /**
     * 실패율 계산
     */
    public double getFailureRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) failureCount / totalCount * 100.0;
    }

    /**
     * 통계 초기화
     */
    public void reset() {
        this.successCount = 0L;
        this.failureCount = 0L;
        this.totalCount = 0L;
    }

    /**
     * 다른 통계와 병합
     */
    public void merge(NotificationStatistics other) {
        this.successCount += other.successCount;
        this.failureCount += other.failureCount;
        this.totalCount += other.totalCount;
    }
}
