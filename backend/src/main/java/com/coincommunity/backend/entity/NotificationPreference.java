package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자별 알림 설정 엔티티
 * 사용자가 받고 싶은 알림 유형을 관리합니다.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_preference_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "websocket_enabled", nullable = false)
    @Builder.Default
    private Boolean websocketEnabled = true;

    /**
     * 생성자
     */
    public NotificationPreference(User user, String notificationType) {
        this.user = user;
        this.notificationType = notificationType;
        this.isEnabled = true;
        this.pushEnabled = true;
        this.emailEnabled = false;
        this.websocketEnabled = true;
    }

    /**
     * 알림이 활성화되어 있는지 확인
     */
    public boolean isNotificationEnabled() {
        return isEnabled != null && isEnabled;
    }

    /**
     * 푸시 알림이 활성화되어 있는지 확인
     */
    public boolean isPushNotificationEnabled() {
        return isNotificationEnabled() && pushEnabled != null && pushEnabled;
    }

    /**
     * 웹소켓 알림이 활성화되어 있는지 확인
     */
    public boolean isWebSocketNotificationEnabled() {
        return isNotificationEnabled() && websocketEnabled != null && websocketEnabled;
    }

    /**
     * 이메일 알림이 활성화되어 있는지 확인
     */
    public boolean isEmailNotificationEnabled() {
        return isNotificationEnabled() && emailEnabled != null && emailEnabled;
    }

    /**
     * 알림 설정 업데이트
     */
    public void updateSettings(Boolean enabled, Boolean push, Boolean email, Boolean websocket) {
        if (enabled != null) {
            this.isEnabled = enabled;
        }
        if (push != null) {
            this.pushEnabled = push;
        }
        if (email != null) {
            this.emailEnabled = email;
        }
        if (websocket != null) {
            this.websocketEnabled = websocket;
        }
    }
}
