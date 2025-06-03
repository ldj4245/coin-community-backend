package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 코인 분석 및 예측 엔티티
 * 사용자들의 코인 분석, 예측, 투자 의견 등을 저장
 */
@Entity
@Table(name = "coin_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinAnalysis extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "coin_id", nullable = false)
    private String coinId; // BTC, ETH 등
    
    @Column(nullable = false)
    private String coinName; // 코인 이름
    
    @Column(nullable = false, length = 200)
    private String title; // 분석 제목
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 분석 내용
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisType analysisType; // 분석 유형
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionPeriod predictionPeriod; // 예측 기간
    
    @Column(precision = 20, scale = 8)
    private BigDecimal currentPrice; // 분석 작성시 현재 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal targetPrice; // 목표 가격
    
    @Column(precision = 10, scale = 2)
    private BigDecimal expectedReturnPercent; // 예상 수익률 (%)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentRecommendation recommendation; // 투자 추천도
    
    @Column(name = "risk_level")
    @Builder.Default
    private Integer riskLevel = 3; // 위험도 (1: 낮음 ~ 5: 높음)
    
    @Column(name = "confidence_level")
    @Builder.Default
    private Integer confidenceLevel = 3; // 확신도 (1: 낮음 ~ 5: 높음)
    
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0; // 조회수
    
    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0; // 좋아요 수
    
    @Column(name = "bookmark_count")
    @Builder.Default
    private Integer bookmarkCount = 0; // 북마크 수
    
    @Column(name = "accuracy_score")
    private BigDecimal accuracyScore; // 예측 정확도 점수 (사후 계산)
    
    @Column(name = "is_featured")
    @Builder.Default
    private boolean isFeatured = false; // 추천 분석 여부
    
    @Column(name = "is_verified")
    @Builder.Default
    private boolean isVerified = false; // 검증된 분석 여부
    
    @Column(columnDefinition = "TEXT")
    private String tags; // 태그 (JSON 또는 쉼표 구분)
    
    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
    
    /**
     * 북마크 수 증가
     */
    public void incrementBookmarkCount() {
        this.bookmarkCount++;
    }
    
    /**
     * 북마크 수 감소
     */
    public void decrementBookmarkCount() {
        if (this.bookmarkCount > 0) {
            this.bookmarkCount--;
        }
    }
    
    /**
     * 예측 정확도 계산 및 업데이트
     */
    public void calculateAccuracy(BigDecimal actualPrice) {
        if (targetPrice != null && currentPrice != null) {
            BigDecimal predictedChange = targetPrice.subtract(currentPrice)
                    .divide(currentPrice, 4, RoundingMode.HALF_UP);
            BigDecimal actualChange = actualPrice.subtract(currentPrice)
                    .divide(currentPrice, 4, RoundingMode.HALF_UP);
            
            // 예측 정확도 계산 (0~100점)
            BigDecimal accuracy = BigDecimal.valueOf(100)
                    .subtract(predictedChange.subtract(actualChange).abs().multiply(BigDecimal.valueOf(100)));
            
            this.accuracyScore = accuracy.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        }
    }
    
    /**
     * 분석 유형
     */
    public enum AnalysisType {
        TECHNICAL("기술적 분석"),
        FUNDAMENTAL("펀더멘털 분석"),
        NEWS("뉴스 분석"),
        MARKET_SENTIMENT("시장 심리 분석"),
        COMBINED("종합 분석");
        
        private final String description;
        
        AnalysisType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 예측 기간
     */
    public enum PredictionPeriod {
        SHORT_TERM("단기 (1주일)"),
        MEDIUM_TERM("중기 (1개월)"),
        LONG_TERM("장기 (3개월+)");
        
        private final String description;
        
        PredictionPeriod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 투자 추천도
     */
    public enum InvestmentRecommendation {
        STRONG_BUY("적극 매수"),
        BUY("매수"),
        HOLD("보유"),
        SELL("매도"),
        STRONG_SELL("적극 매도");
        
        private final String description;
        
        InvestmentRecommendation(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 예측 방향
     */
    public enum PredictionDirection {
        UP("상승"),
        DOWN("하락"),
        SIDEWAYS("횡보");
        
        private final String description;
        
        PredictionDirection(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 시간 프레임
     */
    public enum TimeFrame {
        ONE_DAY("1일"),
        ONE_WEEK("1주일"),
        ONE_MONTH("1개월"),
        THREE_MONTHS("3개월"),
        SIX_MONTHS("6개월"),
        ONE_YEAR("1년");
        
        private final String description;
        
        TimeFrame(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 예측 상태
     */
    public enum PredictionStatus {
        PENDING("예측 대기중"),
        ACTIVE("진행중"),
        EXPIRED("만료됨"),
        ACCURATE("정확함"),
        INACCURATE("부정확함");
        
        private final String description;
        
        PredictionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
