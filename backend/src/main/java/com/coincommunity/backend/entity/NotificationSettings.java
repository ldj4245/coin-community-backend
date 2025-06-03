package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자별 알림 설정 엔티티
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 가격 알림 설정
    @Column(name = "price_alert_enabled", nullable = false)
    private Boolean priceAlertEnabled = true;

    // 커뮤니티 알림 설정 (댓글, 좋아요 등)
    @Column(name = "community_alert_enabled", nullable = false)
    private Boolean communityAlertEnabled = true;

    // 뉴스 알림 설정
    @Column(name = "news_alert_enabled", nullable = false)
    private Boolean newsAlertEnabled = true;

    // 분석 알림 설정
    @Column(name = "analysis_alert_enabled", nullable = false)
    private Boolean analysisAlertEnabled = true;

    // 시장 급변 알림 설정
    @Column(name = "market_alert_enabled", nullable = false)
    private Boolean marketAlertEnabled = true;

    // 푸시 알림 활성화
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    // 웹 알림 활성화
    @Column(name = "web_enabled", nullable = false)
    private Boolean webEnabled = true;

    // 알림 중단 시간 (시작 시각, 예: 22)
    @Column(name = "quiet_hours_start")
    private Integer quietHoursStart;

    // 알림 중단 시간 (종료 시각, 예: 8)
    @Column(name = "quiet_hours_end")
    private Integer quietHoursEnd;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 생성자
    public NotificationSettings(Long userId) {
        this.userId = userId;
    }

    /**
     * 특정 알림 타입이 활성화되어 있는지 확인
     */
    public boolean isTypeEnabled(String notificationType) {
        return switch (notificationType.toLowerCase()) {
            case "price", "price_alert" -> Boolean.TRUE.equals(priceAlertEnabled);
            case "community", "community_alert" -> Boolean.TRUE.equals(communityAlertEnabled);
            case "news", "news_alert" -> Boolean.TRUE.equals(newsAlertEnabled);
            case "analysis", "analysis_alert" -> Boolean.TRUE.equals(analysisAlertEnabled);
            case "market", "market_alert" -> Boolean.TRUE.equals(marketAlertEnabled);
            default -> true; // 기본적으로 활성화
        };
    }

    /**
     * 현재 시간이 알림 중단 시간인지 확인
     */
    public boolean isQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        if (quietHoursStart <= quietHoursEnd) {
            // 같은 날 (예: 22시 ~ 23시)
            return currentHour >= quietHoursStart && currentHour < quietHoursEnd;
        } else {
            // 다음 날로 넘어가는 경우 (예: 22시 ~ 8시)
            return currentHour >= quietHoursStart || currentHour < quietHoursEnd;
        }
    }
}
