package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 포트폴리오 아이템 엔티티
 * 포트폴리오 내 개별 코인 보유 정보
 */
@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    
    @Column(name = "coin_id", nullable = false)
    private String coinId; // BTC, ETH 등
    
    @Column(nullable = false)
    private String coinName; // 코인 이름
    
    @Column(nullable = false)
    private String coinSymbol; // 코인 심볼
    
    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity; // 보유 수량
    
    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal averagePrice; // 평균 매수 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal totalInvestment; // 총 투자금액 (수량 * 평균가격)
    
    @Column(precision = 20, scale = 8)
    private BigDecimal currentPrice; // 현재 가격
    
    @Column(precision = 20, scale = 8)
    private BigDecimal currentValue; // 현재 평가금액 (수량 * 현재가격)
    
    @Column(precision = 20, scale = 8)
    private BigDecimal unrealizedGain; // 미실현 손익
    
    @Column(precision = 10, scale = 2)
    private BigDecimal unrealizedGainPercent; // 미실현 손익률 (%)
    
    @Column(name = "first_purchase_date")
    private LocalDateTime firstPurchaseDate; // 최초 매수일
    
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt; // 마지막 가격 업데이트 시점
    
    /**
     * 매수 거래 추가
     */
    public void addPurchase(BigDecimal purchaseQuantity, BigDecimal purchasePrice) {
        BigDecimal currentTotalValue = this.quantity.multiply(this.averagePrice);
        BigDecimal newTotalValue = purchaseQuantity.multiply(purchasePrice);
        
        this.quantity = this.quantity.add(purchaseQuantity);
        this.averagePrice = currentTotalValue.add(newTotalValue)
                .divide(this.quantity, 8, RoundingMode.HALF_UP);
        
        calculateTotalInvestment();
        
        if (this.firstPurchaseDate == null) {
            this.firstPurchaseDate = LocalDateTime.now();
        }
    }
    
    /**
     * 매도 거래 처리
     */
    public void addSale(BigDecimal saleQuantity) {
        if (saleQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("매도 수량이 보유 수량을 초과할 수 없습니다.");
        }
        
        this.quantity = this.quantity.subtract(saleQuantity);
        calculateTotalInvestment();
    }
    
    /**
     * 현재 가격 업데이트 및 손익 계산
     */
    public void updateCurrentPrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        this.currentValue = this.quantity.multiply(newPrice);
        this.unrealizedGain = this.currentValue.subtract(this.totalInvestment);
        
        if (this.totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            this.unrealizedGainPercent = this.unrealizedGain
                    .divide(this.totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.unrealizedGainPercent = BigDecimal.ZERO;
        }
        
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * 총 투자금액 계산
     */
    public void calculateTotalInvestment() {
        this.totalInvestment = this.quantity.multiply(this.averagePrice);
    }
    
    /**
     * 보유 비중 계산 (포트폴리오 대비)
     */
    public BigDecimal getWeightPercent() {
        if (portfolio == null || portfolio.getCurrentValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return this.currentValue
                .divide(portfolio.getCurrentValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 포트폴리오 설정
     */
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    /**
     * 총 투자 금액 반환
     */
    public BigDecimal getTotalInvestment() {
        return this.totalInvestment != null ? this.totalInvestment : BigDecimal.ZERO;
    }
    
    /**
     * 현재 가치 반환
     */
    public BigDecimal getCurrentValue() {
        return this.currentValue != null ? this.currentValue : BigDecimal.ZERO;
    }
}
