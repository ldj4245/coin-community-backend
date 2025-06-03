package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 김치프리미엄 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "김치프리미엄 정보")
public class KimchiPremiumDto {

    @Schema(description = "코인 심볼", example = "BTC")
    private String symbol;

    @Schema(description = "코인 한글명", example = "비트코인")
    private String koreanName;

    @Schema(description = "국내 거래소별 가격 정보")
    private Map<String, ExchangePriceInfo> domesticPrices;

    @Schema(description = "해외 거래소별 가격 정보")
    private Map<String, ExchangePriceInfo> foreignPrices;

    @Schema(description = "김치프리미엄 비율 (%)", example = "3.5")
    private BigDecimal premiumRate;

    @Schema(description = "김치프리미엄 절대값 (KRW)", example = "1500000")
    private BigDecimal premiumAmount;

    @Schema(description = "기준 해외 거래소", example = "BINANCE")
    private String baseExchange;

    @Schema(description = "최고가 국내 거래소", example = "UPBIT")
    private String highestDomesticExchange;

    @Schema(description = "최저가 국내 거래소", example = "BITHUMB")
    private String lowestDomesticExchange;

    @Schema(description = "업데이트 시간")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래소별 가격 정보")
    public static class ExchangePriceInfo {
        
        @Schema(description = "거래소명", example = "UPBIT")
        private String exchange;

        @Schema(description = "가격 (KRW)", example = "45000000")
        private BigDecimal priceKrw;

        @Schema(description = "가격 (USD)", example = "35000")
        private BigDecimal priceUsd;

        @Schema(description = "24시간 변동률 (%)", example = "2.5")
        private BigDecimal changeRate24h;

        @Schema(description = "거래량", example = "1234.5678")
        private BigDecimal volume24h;

        @Schema(description = "거래소 상태", example = "NORMAL")
        private String status;

        @Schema(description = "마지막 업데이트 시간")
        private LocalDateTime lastUpdated;
    }
}
