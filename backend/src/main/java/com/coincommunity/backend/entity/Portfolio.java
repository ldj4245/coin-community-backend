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
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 포트폴리오 엔티티
 * 코인 커뮤니티의 핵심 기능 - 사용자의 코인 보유 현황과 수익률 추적
 */
@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;
    
    @Column(nullable = false)
    private String name; // 포트폴리오 이름 (예: "메인 포트폴리오", "장기투자")
    
    @Column(columnDefinition = "TEXT")
    private String description; // 포트폴리오 설명
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PortfolioItem> items = new ArrayList<>();
    
    @Column(precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal totalInvestment = BigDecimal.ZERO; // 총 투자금액
    
    @Column(precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal currentValue = BigDecimal.ZERO; // 현재 평가금액
    
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalReturnPercent = BigDecimal.ZERO; // 총 수익률 (%)
    
    @Column(precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal totalReturnAmount = BigDecimal.ZERO; // 총 수익금액
    
    @Column(name = "is_public")
    @Builder.Default
    private boolean isPublic = false; // 공개 여부 (다른 사용자가 볼 수 있는지)
    
    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false; // 기본 포트폴리오 여부
    
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt; // 마지막 수익률 계산 시점
    
    /**
     * 포트폴리오 아이템 추가
     */
    public void addItem(PortfolioItem item) {
        items.add(item);
        item.setPortfolio(this);
    }
    
    /**
     * 포트폴리오 아이템 제거
     */
    public void removeItem(PortfolioItem item) {
        items.remove(item);
        item.setPortfolio(null);
    }
    
    /**
     * 포트폴리오 수익률 계산 및 업데이트
     */
    public void calculateReturns() {
        BigDecimal totalInvest = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        
        for (PortfolioItem item : items) {
            totalInvest = totalInvest.add(item.getTotalInvestment());
            totalCurrent = totalCurrent.add(item.getCurrentValue());
        }
        
        this.totalInvestment = totalInvest;
        this.currentValue = totalCurrent;
        this.totalReturnAmount = totalCurrent.subtract(totalInvest);
        
        if (totalInvest.compareTo(BigDecimal.ZERO) > 0) {
            this.totalReturnPercent = totalReturnAmount
                    .divide(totalInvest, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.totalReturnPercent = BigDecimal.ZERO;
        }
        
        this.lastCalculatedAt = LocalDateTime.now();
    }
    
    /**
     * 기본 포트폴리오로 설정
     */
    public void setAsDefault() {
        this.isDefault = true;
    }
    
    /**
     * 포트폴리오 공개/비공개 전환
     */
    public void togglePublicStatus() {
        this.isPublic = !this.isPublic;
    }
    
    /**
     * 현재 포트폴리오 총 가치 반환
     */
    public BigDecimal getCurrentValue() {
        return this.currentValue != null ? this.currentValue : BigDecimal.ZERO;
    }
    
    /**
     * 총 투자 금액 반환
     */
    public BigDecimal getTotalInvestment() {
        return this.totalInvestment != null ? this.totalInvestment : BigDecimal.ZERO;
    }
    
    /**
     * 사용자와의 관계 설정
     */
    public void addToUser(User user) {
        this.user = user;
        if (user != null && !user.getPortfolios().contains(this)) {
            user.getPortfolios().add(this);
        }
    }
}
