package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.CoinPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 코인 가격 정보 관련 DTO 클래스
 */
public class CoinPriceDto {

    /**
     * 코인 가격 응답 DTO
     */
    @Getter
    @Setter // Setter 추가
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoinPriceResponse {
        private String coinId;
        private String koreanName;
        private String englishName;
        private BigDecimal currentPrice;
        private BigDecimal priceChangePercent;
        private BigDecimal volume24h;
        private String exchange;
        private LocalDateTime lastUpdated;
        private BigDecimal highPrice24h;
        private BigDecimal lowPrice24h;
        private BigDecimal marketCap;
        private String note; // 노트 필드 추가

        /**
         * CoinPrice 엔티티로부터 CoinPriceResponse DTO를 생성합니다.
         */
        public static CoinPriceResponse from(CoinPrice coinPrice) {
            return CoinPriceResponse.builder()
                    .coinId(coinPrice.getCoinId())
                    .koreanName(coinPrice.getKoreanName())
                    .englishName(coinPrice.getEnglishName())
                    .currentPrice(coinPrice.getCurrentPrice())
                    .priceChangePercent(coinPrice.getPriceChangePercent())
                    .volume24h(coinPrice.getVolume24h())
                    .exchange(coinPrice.getExchange())
                    .lastUpdated(coinPrice.getLastUpdated())
                    .highPrice24h(coinPrice.getHighPrice24h())
                    .lowPrice24h(coinPrice.getLowPrice24h())
                    .marketCap(coinPrice.getMarketCap())
                    .build();
        }
        
        /**
         * CoinPrice 엔티티 목록으로부터 CoinPriceResponse DTO 목록을 생성합니다.
         */
        public static List<CoinPriceResponse> fromList(List<CoinPrice> coinPrices) {
            return coinPrices.stream()
                    .map(CoinPriceResponse::from)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 실시간 코인 가격 업데이트를 위한 WebSocket 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeUpdate {
        private String coinId;
        private String koreanName;
        private String englishName;
        private BigDecimal currentPrice;
        private BigDecimal priceChangePercent;
        private String exchange;
        private LocalDateTime timestamp;
        
        /**
         * CoinPrice 엔티티로부터 RealtimeUpdate DTO를 생성합니다.
         */
        public static RealtimeUpdate from(CoinPrice coinPrice) {
            return RealtimeUpdate.builder()
                    .coinId(coinPrice.getCoinId())
                    .koreanName(coinPrice.getKoreanName())
                    .englishName(coinPrice.getEnglishName())
                    .currentPrice(coinPrice.getCurrentPrice())
                    .priceChangePercent(coinPrice.getPriceChangePercent())
                    .exchange(coinPrice.getExchange())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
