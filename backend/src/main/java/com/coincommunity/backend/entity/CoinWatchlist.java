package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 코인 관심 종목 엔티티
 * 사용자가 관심있어하는 코인 목록 관리
 */
@Entity
@Table(name = "coin_watchlist", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coin_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinWatchlist extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "coin_id", nullable = false)
    private String coinId; // BTC, ETH 등
    
    @Column(nullable = false)
    private String coinName; // 코인 이름
    
    @Column(nullable = false)
    private String coinSymbol; // 코인 심볼
    
    @Column(precision = 20, scale = 8)
    private BigDecimal targetPrice; // 목표 가격 (알림용)
    
    @Column(precision = 20, scale = 8)
    private BigDecimal alertPriceHigh; // 상한 알림 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal alertPriceLow; // 하한 알림 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal targetHighPrice; // 목표 상한 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal targetLowPrice; // 목표 하한 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal currentPrice; // 현재 가격
    
    @Column(precision = 10, scale = 4)
    private BigDecimal priceChangePercent; // 가격 변동률 (%)
    
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt; // 마지막 알림 트리거 시간
    
    @Builder.Default
    @Column(name = "price_alert_enabled")
    private boolean alertEnabled = false; // 가격 알림 활성화 여부
    
    @Builder.Default
    @Column(name = "news_alert_enabled")
    private boolean newsAlertEnabled = false; // 뉴스 알림 활성화 여부
    
    @Column(columnDefinition = "TEXT")
    private String memo; // 메모
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WatchlistCategory category = WatchlistCategory.GENERAL; // 카테고리
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0; // 정렬 순서
    
    /**
     * 가격 알림 활성화/비활성화
     */
    public void togglePriceAlert() {
        this.alertEnabled = !this.alertEnabled;
    }
    
    /**
     * 뉴스 알림 활성화/비활성화
     */
    public void toggleNewsAlert() {
        this.newsAlertEnabled = !this.newsAlertEnabled;
    }
    
    /**
     * 목표 가격 설정
     */
    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
        this.alertEnabled = true;
    }
    
    /**
     * 알림 가격 범위 설정
     */
    public void setAlertPriceRange(BigDecimal lowPrice, BigDecimal highPrice) {
        this.alertPriceLow = lowPrice;
        this.alertPriceHigh = highPrice;
        this.alertEnabled = true;
    }
    
    /**
     * 목표 상한 가격 설정
     */
    public void setTargetHighPrice(BigDecimal targetHighPrice) {
        this.targetHighPrice = targetHighPrice;
    }
    
    /**
     * 목표 하한 가격 설정
     */
    public void setTargetLowPrice(BigDecimal targetLowPrice) {
        this.targetLowPrice = targetLowPrice;
    }
    
    /**
     * 현재 가격 설정
     */
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    /**
     * 알림 활성화 설정
     */
    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }
    
    /**
     * 알림 활성화 상태 확인
     */
    public boolean isAlertEnabled() {
        return this.alertEnabled;
    }

    /**
     * 목표 상한 가격 조회
     */
    public BigDecimal getTargetHighPrice() {
        return this.targetHighPrice;
    }
    
    /**
     * 목표 하한 가격 조회
     */
    public BigDecimal getTargetLowPrice() {
        return this.targetLowPrice;
    }
    
    /**
     * 현재 가격 조회
     */
    public BigDecimal getCurrentPrice() {
        return this.currentPrice;
    }
    
    /**
     * 가격 변동률 조회
     */
    public BigDecimal getPriceChangePercent() {
        return this.priceChangePercent;
    }
    
    /**
     * 마지막 알림 트리거 시간 조회
     */
    public LocalDateTime getLastTriggeredAt() {
        return this.lastTriggeredAt;
    }
    
    /**
     * 관심종목 카테고리
     */
    public enum WatchlistCategory {
        GENERAL("일반"),
        MAJOR_COIN("주요코인"),
        ALTCOIN("알트코인"),
        DEFI("디파이"),
        NFT("NFT"),
        MEME("밈코인"),
        TRADING("트레이딩");
        
        private final String description;
        
        WatchlistCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
