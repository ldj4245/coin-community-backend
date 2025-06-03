package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

/**
 * 사용자 점수 및 레벨 시스템 엔티티
 * coinpan.com의 레벨 시스템을 참고한 커뮤니티 활동 기반 점수 시스템
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
    @Column(name = "analysis_score")
    private Integer analysisScore = 0; // 분석 글 점수
    
    @Builder.Default
    @Column(name = "prediction_score")
    private Integer predictionScore = 0; // 예측 정확도 점수
    
    @Builder.Default
    @Column(name = "community_score")
    private Integer communityScore = 0; // 커뮤니티 활동 점수
    
    @Builder.Default
    @Column(name = "portfolio_score")
    private Integer portfolioScore = 0; // 포트폴리오 수익률 점수
    
    @Builder.Default
    @Column(name = "trading_score")
    private Integer tradingScore = 0; // 트레이딩 점수
    
    @Builder.Default
    @Column(name = "level")
    private Integer level = 1; // 사용자 레벨
    
    @Builder.Default
    @Column(name = "level_name")
    private String levelName = "새싹투자자"; // 레벨명
    
    @Builder.Default
    @Column(name = "next_level_score")
    private Integer nextLevelScore = 100; // 다음 레벨 필요 점수
    
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
    @Column(name = "analysis_accuracy_rate")
    private Double analysisAccuracyRate = 0.0; // 분석 정확도 (%)
    
    @Builder.Default
    @Column(name = "consecutive_accurate_predictions")
    private Integer consecutiveAccuratePredictions = 0; // 연속 정확한 예측 수
    
    @Builder.Default
    @Column(name = "reputation_score")
    private Integer reputationScore = 0; // 평판 점수
    
    @Builder.Default
    @Column(name = "is_expert")
    private boolean isExpert = false; // 전문가 인증 여부
    
    @Column(name = "expert_category")
    private String expertCategory; // 전문 분야 (기술분석, 펀더멘털 등)
    
    @Builder.Default
    @Column(name = "consecutive_days")
    private Integer consecutiveDays = 0; // 연속 출석일
    
    @Column(name = "last_level_up_at")
    private java.time.LocalDateTime lastLevelUpAt; // 마지막 레벨업 시간
    
    @Column(name = "level_title")
    private String levelTitle; // 레벨 제목
    
    @Builder.Default
    @Column(name = "next_level_required_score")
    private Integer nextLevelRequiredScore = 100; // 다음 레벨 필요 점수
    
    @Column(name = "last_updated_at")
    private java.time.LocalDateTime lastUpdatedAt; // 마지막 업데이트 시간
    
    /**
     * 게시글 작성시 점수 추가
     */
    public void addPostScore(int score) {
        this.communityScore += score;
        this.postCount++;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 댓글 작성시 점수 추가
     */
    public void addCommentScore(int score) {
        this.communityScore += score;
        this.commentCount++;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 좋아요 받았을 때 점수 추가
     */
    public void addLikeScore(int score) {
        this.communityScore += score;
        this.likeReceivedCount++;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 분석 글 작성시 점수 추가
     */
    public void addAnalysisScore(int score) {
        this.analysisScore += score;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 예측 정확도에 따른 점수 추가
     */
    public void addPredictionScore(int score, boolean isAccurate) {
        this.predictionScore += score;
        
        if (isAccurate) {
            this.consecutiveAccuratePredictions++;
            // 연속 정확한 예측시 보너스 점수
            if (this.consecutiveAccuratePredictions >= 5) {
                this.predictionScore += 50; // 보너스 점수
            }
        } else {
            this.consecutiveAccuratePredictions = 0;
        }
        
        updateAccuracyRate();
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 포트폴리오 수익률에 따른 점수 추가
     */
    public void addPortfolioScore(int score) {
        this.portfolioScore += score;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 활동 점수 추가
     */
    public void addActivityScore(int score) {
        this.communityScore += score;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 커뮤니티 점수 추가
     */
    public void addCommunityScore(int score) {
        this.communityScore += score;
        updateTotalScore();
        updateLevel();
    }
    
    /**
     * 총 점수 계산 및 업데이트
     */
    public void calculateTotalScore() {
        this.totalScore = this.analysisScore + this.predictionScore + 
                         this.communityScore + this.portfolioScore + this.reputationScore;
    }
    
    /**
     * 레벨 제목 설정
     */
    public void setLevelTitle(String levelTitle) {
        this.levelTitle = levelTitle;
    }
    
    /**
     * 레벨 제목 반환
     */
    public String getLevelTitle() {
        return this.levelTitle;
    }
    
    /**
     * 마지막 레벨업 시간 설정
     */
    public void setLastLevelUpAt(java.time.LocalDateTime lastLevelUpAt) {
        this.lastLevelUpAt = lastLevelUpAt;
    }
    
    /**
     * 다음 레벨 필요 점수 설정
     */
    public void setNextLevelRequiredScore(Integer nextLevelRequiredScore) {
        this.nextLevelRequiredScore = nextLevelRequiredScore;
    }
    
    /**
     * 다음 레벨 필요 점수 반환
     */
    public Integer getNextLevelRequiredScore() {
        return this.nextLevelRequiredScore;
    }
    
    /**
     * 연속 출석일 설정
     */
    public void setConsecutiveDays(int consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }
    
    /**
     * 연속 출석일 반환
     */
    public int getConsecutiveDays() {
        return this.consecutiveDays;
    }
    
    /**
     * 마지막 업데이트 시간 반환
     */
    public java.time.LocalDateTime getLastUpdatedAt() {
        return this.lastUpdatedAt;
    }

    /**
     * 총 점수 업데이트
     */
    private void updateTotalScore() {
        this.totalScore = this.analysisScore + this.predictionScore + 
                         this.communityScore + this.portfolioScore + this.reputationScore;
    }
    
    /**
     * 레벨 업데이트
     */
    private void updateLevel() {
        int newLevel = calculateLevel(this.totalScore);
        if (newLevel > this.level) {
            this.level = newLevel;
            this.levelName = getLevelName(newLevel);
            this.nextLevelScore = getNextLevelScore(newLevel);
            
            // 전문가 인증 조건 확인 (레벨 10 이상, 정확도 75% 이상)
            if (newLevel >= 10 && this.analysisAccuracyRate >= 75.0) {
                this.isExpert = true;
            }
        }
    }
    
    /**
     * 분석 정확도 업데이트
     */
    private void updateAccuracyRate() {
        // 실제 구현시에는 분석 결과 통계를 기반으로 계산
        // 여기서는 간단한 예시만 제공
    }
    
    /**
     * 점수에 따른 레벨 계산
     */
    private int calculateLevel(int score) {
        if (score < 100) return 1;
        else if (score < 300) return 2;
        else if (score < 600) return 3;
        else if (score < 1000) return 4;
        else if (score < 1500) return 5;
        else if (score < 2100) return 6;
        else if (score < 2800) return 7;
        else if (score < 3600) return 8;
        else if (score < 4500) return 9;
        else if (score < 5500) return 10;
        else return Math.min(20, 10 + (score - 5500) / 1000);
    }
    
    /**
     * 레벨명 반환
     */
    private String getLevelName(int level) {
        switch (level) {
            case 1: return "새싹투자자";
            case 2: return "초보투자자";
            case 3: return "일반투자자";
            case 4: return "숙련투자자";
            case 5: return "중급투자자";
            case 6: return "고급투자자";
            case 7: return "베테랑투자자";
            case 8: return "전문투자자";
            case 9: return "마스터투자자";
            case 10: return "구루투자자";
            default: return "전설의투자자";
        }
    }
    
    /**
     * 다음 레벨 필요 점수 계산
     */
    private int getNextLevelScore(int currentLevel) {
        int[] levelThresholds = {100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500};
        
        if (currentLevel < levelThresholds.length) {
            return levelThresholds[currentLevel];
        } else {
            return 5500 + (currentLevel - 9) * 1000;
        }
    }
}
