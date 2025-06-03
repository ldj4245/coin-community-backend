package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래소별 시세 비교 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "거래소별 시세 비교 정보")
public class ExchangeComparisonDto {

    @Schema(description = "코인 심볼", example = "BTC")
    private String symbol;

    @Schema(description = "코인 한글명", example = "비트코인")
    private String koreanName;

    @Schema(description = "전체 거래소 수", example = "6")
    private int totalExchanges;

    @Schema(description = "국내 거래소 수", example = "4")
    private int domesticExchanges;

    @Schema(description = "해외 거래소 수", example = "2")
    private int foreignExchanges;

    @Schema(description = "모든 거래소 시세 목록")
    private List<ExchangePriceDto> exchangePrices;

    @Schema(description = "최고가 거래소")
    private ExchangeHighLow highest;

    @Schema(description = "최저가 거래소")
    private ExchangeHighLow lowest;

    @Schema(description = "평균 가격 (KRW)", example = "44050000")
    private BigDecimal averagePrice;

    @Schema(description = "중간값 (KRW)", example = "44100000")
    private BigDecimal medianPrice;

    @Schema(description = "표준편차", example = "850000.50")
    private BigDecimal standardDeviation;

    @Schema(description = "최대 가격 차이 (KRW)", example = "1700000")
    private BigDecimal maxPriceDifference;

    @Schema(description = "최대 가격 차이 비율 (%)", example = "3.93")
    private BigDecimal maxPriceDifferenceRate;

    @Schema(description = "현재 USD 환율", example = "1350.50")
    private BigDecimal usdExchangeRate;

    @Schema(description = "업데이트 시간")
    private LocalDateTime updatedAt;

    @Schema(description = "시세 신뢰도 점수 (1-100)", example = "92")
    private Integer reliabilityScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최고가/최저가 거래소 정보")
    public static class ExchangeHighLow {
        
        @Schema(description = "거래소명", example = "UPBIT")
        private String exchangeName;

        @Schema(description = "거래소 한글명", example = "업비트")
        private String exchangeKoreanName;

        @Schema(description = "가격 (KRW)", example = "45000000")
        private BigDecimal price;

        @Schema(description = "변화율 (%)", example = "2.35")
        private BigDecimal changeRate;

        @Schema(description = "24시간 거래량", example = "123.45")
        private BigDecimal volume;

        @Schema(description = "거래소 타입", example = "DOMESTIC")
        private ExchangePriceDto.ExchangeType exchangeType;
    }
}
