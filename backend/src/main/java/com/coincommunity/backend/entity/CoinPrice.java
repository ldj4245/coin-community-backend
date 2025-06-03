package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 코인 가격 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "coin_prices")
@Getter
@Setter
@NoArgsConstructor
public class CoinPrice {
    
    @Id
    @Column(name = "coin_id")
    private String coinId; // BTC, ETH 등
    
    @Column(nullable = false)
    private String koreanName;
    
    @Column(nullable = false)
    private String englishName;
    
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal currentPrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal priceChangePercent;
    
    @Column(precision = 30, scale = 8)
    private BigDecimal volume24h;
    
    @Column(nullable = false)
    private String exchange; // UPBIT, BITHUMB 등
    
    @UpdateTimestamp
    private LocalDateTime lastUpdated;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal highPrice24h;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal lowPrice24h;
    
    @Column(precision = 30, scale = 8)
    private BigDecimal marketCap;
}
