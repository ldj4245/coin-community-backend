package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String nickname;
    
    private String avatarUrl;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @Builder.Default
    private Integer point = 0;
    
    @Builder.Default
    private boolean emailVerified = false;
    
    // FCM 푸시 알림 토큰
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;
    
    // 활동 포인트/레벨 관련 필드
    @Builder.Default
    private Integer activityLevel = 1;
    
    // 전문가 인증 여부
    @Column(name = "is_expert")
    @Builder.Default
    private Boolean isExpert = false;
    
    // 트레이딩 점수
    @Column(name = "trading_score")
    @Builder.Default
    private Integer tradingScore = 0;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    // 새로운 코인 커뮤니티 특화 기능 관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Portfolio> portfolios = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CoinWatchlist> watchlists = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CoinAnalysis> analyses = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserScore userScore;
    
    /**
     * 사용자의 비밀번호를 변경합니다.
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
    
    /**
     * 사용자의 상태를 변경합니다.
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
    
    /**
     * 사용자 프로필을 업데이트합니다.
     */
    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
    
    /**
     * 활동 포인트를 추가합니다.
     */
    public void addPoints(int points) {
        this.point += points;
        updateActivityLevel();
    }
    
    /**
     * 활동 레벨을 업데이트합니다.
     */
    private void updateActivityLevel() {
        if (this.point >= 1000) {
            this.activityLevel = 5;
        } else if (this.point >= 500) {
            this.activityLevel = 4;
        } else if (this.point >= 200) {
            this.activityLevel = 3;
        } else if (this.point >= 50) {
            this.activityLevel = 2;
        } else {
            this.activityLevel = 1;
        }
    }
    
    /**
     * FCM 토큰을 업데이트합니다.
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    /**
     * FCM 토큰을 조회합니다.
     */
    public String getFcmToken() {
        return this.fcmToken;
    }
}
