package com.coincommunity.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 트렌딩 코인 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingCoinDto {
    
    private String symbol;
    private String name;
    private String koreanName;
    private BigDecimal currentPrice;
    private BigDecimal priceChangePercent24h;
    private BigDecimal volume24h;
    private BigDecimal marketCap;
    private Integer communityMentionCount;
    private Integer trendingScore;
    private String imageUrl;
    private LocalDateTime lastUpdated;
    
    /**
     * 트렌딩 코인 정렬 기준
     */
    public enum SortBy {
        PRICE_CHANGE,
        VOLUME,
        MARKET_CAP,
        COMMUNITY_MENTIONS,
        TRENDING_SCORE
    }
}