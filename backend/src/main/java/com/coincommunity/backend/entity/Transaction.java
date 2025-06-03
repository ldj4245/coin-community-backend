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
 * 거래 내역 엔티티
 * 사용자의 코인 매수/매도 거래 기록
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    @Column(name = "coin_id", nullable = false)
    private String coinId; // BTC, ETH 등
    
    @Column(nullable = false)
    private String coinName; // 코인 이름
    
    @Column(nullable = false)
    private String coinSymbol; // 코인 심볼
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // BUY, SELL
    
    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity; // 거래 수량
    
    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal price; // 거래 단가
    
    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal totalAmount; // 총 거래금액 (수량 * 단가)
    
    @Column(precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO; // 거래 수수료
    
    @Column(nullable = false)
    private String exchange; // 거래소 (UPBIT, BINANCE 등)
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate; // 거래 일시
    
    @Column(columnDefinition = "TEXT")
    private String memo; // 거래 메모
    
    @Column(name = "is_realized")
    @Builder.Default
    private boolean isRealized = false; // 실현손익 계산 여부
    
    @Column(precision = 20, scale = 8)
    private BigDecimal realizedGain; // 실현손익 (매도시에만 계산)
    
    @Column(precision = 10, scale = 2)
    private BigDecimal realizedGainPercent; // 실현손익률 (%)
    
    /**
     * 총 거래금액 계산 (수수료 포함)
     */
    public BigDecimal getTotalAmountWithFee() {
        return totalAmount.add(fee);
    }
    
    /**
     * 실현손익 계산 (매도시)
     */
    public void calculateRealizedGain(BigDecimal averageBuyPrice) {
        if (type == TransactionType.SELL) {
            BigDecimal gainPerUnit = price.subtract(averageBuyPrice);
            this.realizedGain = gainPerUnit.multiply(quantity);
            
            if (averageBuyPrice.compareTo(BigDecimal.ZERO) > 0) {
                this.realizedGainPercent = gainPerUnit
                        .divide(averageBuyPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            this.isRealized = true;
        }
    }
    
    /**
     * 거래 타입
     */
    public enum TransactionType {
        BUY("매수"),
        SELL("매도");
        
        private final String description;
        
        TransactionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
