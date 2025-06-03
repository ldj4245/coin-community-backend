package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 알림 엔티티
 */
@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 받는 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 알림 제목
     */
    @Column(nullable = false)
    private String title;

    /**
     * 알림 메시지
     */
    @Column(nullable = false)
    private String message;

    /**
     * 알림 타입 (comment, like, follow, news, price-alert 등)
     */
    @Column(nullable = false)
    private String type;

    /**
     * 관련 리소스 ID (게시글 ID, 댓글 ID 등 - 알림과 관련된 리소스)
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * 알림을 읽었는지 여부
     */
    @Column(nullable = false)
    private boolean isRead;

    /**
     * 알림을 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 알림을 읽음으로 표시
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
