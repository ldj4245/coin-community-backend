package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

/**
 * 사용자 활동 점수 시스템 엔티티 (간소화)
 * 코인 커뮤니티에 필요한 핵심 기능만 제공
 */
@Entity
@Table(name = "user_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserScore extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Builder.Default
    @Column(name = "total_score")
    private Integer totalScore = 0; // 총 점수
    
    @Builder.Default
    @Column(name = "level")
    private Integer level = 1; // 사용자 레벨 (1-10)
    
    @Builder.Default
    @Column(name = "post_count")
    private Integer postCount = 0; // 작성한 게시글 수
    
    @Builder.Default
    @Column(name = "comment_count")
    private Integer commentCount = 0; // 작성한 댓글 수
    
    @Builder.Default
    @Column(name = "like_received_count")
    private Integer likeReceivedCount = 0; // 받은 좋아요 수
    
    @Builder.Default
    @Column(name = "analysis_count")
    private Integer analysisCount = 0; // 작성한 분석 수
    
    @Builder.Default
    @Column(name = "analysis_accuracy_rate")
    private Double analysisAccuracyRate = 0.0; // 분석 정확도 (%)
    
    @Builder.Default
    @Column(name = "is_verified")
    private boolean isVerified = false; // 인증된 사용자 여부
    
    @Column(name = "verification_type")
    private String verificationType; // 인증 타입 (TRADER, ANALYST, EXPERT 등)
    
    /**
     * 게시글 작성 시 점수 추가
     */
    public void addPostScore() {
        this.postCount++;
        this.totalScore += 10; // 게시글당 10점
        updateLevel();
    }
    
    /**
     * 댓글 작성 시 점수 추가
     */
    public void addCommentScore() {
        this.commentCount++;
        this.totalScore += 2; // 댓글당 2점
        updateLevel();
    }
    
    /**
     * 좋아요 받았을 때 점수 추가
     */
    public void addLikeScore() {
        this.likeReceivedCount++;
        this.totalScore += 5; // 좋아요당 5점
        updateLevel();
    }
    
    /**
     * 분석 글 작성 시 점수 추가
     */
    public void addAnalysisScore() {
        this.analysisCount++;
        this.totalScore += 20; // 분석글당 20점
        updateLevel();
    }
    
    /**
     * 분석 정확도 업데이트
     */
    public void updateAnalysisAccuracy(double accuracy) {
        this.analysisAccuracyRate = accuracy;
        
        // 정확도가 높은 경우 인증 부여
        if (accuracy >= 75.0 && this.analysisCount >= 10) {
            this.isVerified = true;
            this.verificationType = "ANALYST";
        }
    }
    
    /**
     * 레벨 업데이트 (1-10 레벨)
     */
    private void updateLevel() {
        int newLevel = Math.min(10, Math.max(1, this.totalScore / 100 + 1));
        this.level = newLevel;
    }
    
    /**
     * 레벨명 반환
     */
    public String getLevelName() {
        switch (this.level) {
            case 1: return "새싹";
            case 2: return "초보";
            case 3: return "일반";
            case 4: return "숙련";
            case 5: return "중급";
            case 6: return "고급";
            case 7: return "베테랑";
            case 8: return "전문가";
            case 9: return "마스터";
            case 10: return "구루";
            default: return "새싹";
        }
    }
    
    /**
     * 다음 레벨까지 필요한 점수
     */
    public int getScoreToNextLevel() {
        if (this.level >= 10) return 0;
        return (this.level * 100) - this.totalScore;
    }
}
