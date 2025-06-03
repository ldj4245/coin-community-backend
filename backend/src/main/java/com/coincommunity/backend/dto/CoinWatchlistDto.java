package com.coincommunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관심종목 관련 DTO 클래스
 * 사용자의 관심 암호화폐 관리를 위한 데이터 전송 객체
 */
public class CoinWatchlistDto {

    /**
     * 관심종목 추가 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관심종목 추가 요청")
    public static class AddRequest {
        
        @NotBlank(message = "코인 심볼은 필수입니다")
        @Size(max = 20, message = "코인 심볼은 20자를 초과할 수 없습니다")
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
        @Schema(description = "카테고리", example = "메이저 코인")
        private String category;
        
        @Schema(description = "가격 알림 활성화", example = "true")
        private Boolean priceAlertEnabled;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표가는 0보다 커야 합니다")
        @Schema(description = "목표가", example = "60000000")
        private BigDecimal targetPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "손절가는 0보다 커야 합니다")
        @Schema(description = "손절가", example = "45000000")
        private BigDecimal stopLossPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 상한가는 0보다 커야 합니다")
        @Schema(description = "목표 상한가", example = "70000000")
        private BigDecimal targetHighPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 하한가는 0보다 커야 합니다")
        @Schema(description = "목표 하한가", example = "40000000")
        private BigDecimal targetLowPrice;
        
        @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다")
        @Schema(description = "메모", example = "장기 투자 관심 종목")
        private String notes;
        
        @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다")
        @Schema(description = "메모", example = "장기 투자 관심 종목")
        private String memo;
        
        @Schema(description = "알림 활성화", example = "true")
        private Boolean isAlertEnabled;
    }

    /**
     * 관심종목 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관심종목 업데이트 요청")
    public static class UpdateRequest {
        
        @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
        @Schema(description = "카테고리")
        private String category;
        
        @Schema(description = "가격 알림 활성화")
        private Boolean priceAlertEnabled;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표가는 0보다 커야 합니다")
        @Schema(description = "목표가")
        private BigDecimal targetPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "손절가는 0보다 커야 합니다")
        @Schema(description = "손절가")
        private BigDecimal stopLossPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 상한가는 0보다 커야 합니다")
        @Schema(description = "목표 상한가")
        private BigDecimal targetHighPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 하한가는 0보다 커야 합니다")
        @Schema(description = "목표 하한가")
        private BigDecimal targetLowPrice;
        
        @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다")
        @Schema(description = "메모")
        private String notes;
        
        @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다")
        @Schema(description = "메모")
        private String memo;
        
        @Schema(description = "알림 활성화")
        private Boolean isAlertEnabled;
    }

    /**
     * 관심종목 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "관심종목 응답")
    public static class Response {
        
        @Schema(description = "관심종목 ID", example = "1")
        private Long id;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "코인 이름", example = "Bitcoin")
        private String coinName;
        
        @Schema(description = "현재가", example = "55000000")
        private BigDecimal currentPrice;
        
        @Schema(description = "24시간 변동률 (%)", example = "3.5")
        private BigDecimal dailyChangePercentage;
        
        @Schema(description = "가격 변동률 (%)", example = "3.5")
        private BigDecimal priceChangePercent;
        
        @Schema(description = "24시간 거래량", example = "1500000000")
        private BigDecimal volume24h;
        
        @Schema(description = "시가총액", example = "1000000000000")
        private BigDecimal marketCap;
        
        @Schema(description = "카테고리", example = "메이저 코인")
        private String category;
        
        @Schema(description = "가격 알림 활성화", example = "true")
        private Boolean priceAlertEnabled;
        
        @Schema(description = "목표가", example = "60000000")
        private BigDecimal targetPrice;
        
        @Schema(description = "목표 상한가", example = "70000000")
        private BigDecimal targetHighPrice;
        
        @Schema(description = "목표 하한가", example = "40000000")
        private BigDecimal targetLowPrice;
        
        @Schema(description = "손절가", example = "45000000")
        private BigDecimal stopLossPrice;
        
        @Schema(description = "목표가 달성 여부", example = "false")
        private Boolean targetPriceReached;
        
        @Schema(description = "손절가 도달 여부", example = "false")
        private Boolean stopLossPriceReached;
        
        @Schema(description = "마지막 알림 트리거 시간")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastTriggeredAt;
        
        @Schema(description = "메모")
        private String notes;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "추가일시")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "가격 업데이트 시간")
        private LocalDateTime priceUpdatedAt;
    }

    /**
     * 관심종목 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관심종목 요약")
    public static class Summary {
        
        @Schema(description = "관심종목 ID", example = "1")
        private Long id;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "현재가", example = "55000000")
        private BigDecimal currentPrice;
        
        @Schema(description = "24시간 변동률 (%)", example = "3.5")
        private BigDecimal dailyChangePercentage;
        
        @Schema(description = "가격 변동률 (%)", example = "3.5")
        private BigDecimal priceChangePercent;
        
        @Schema(description = "목표가 달성 여부", example = "false")
        private Boolean targetPriceReached;
        
        @Schema(description = "카테고리", example = "메이저 코인")
        private String category;
    }

    /**
     * 관심종목 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관심종목 통계")
    public static class Statistics {
        
        @Schema(description = "총 관심종목 수", example = "15")
        private Long totalWatchlistItems;
        
        @Schema(description = "총 항목 수", example = "15")
        private Long totalItems;
        
        @Schema(description = "카테고리별 분포")
        private List<CategoryStat> categoryStats;
        
        @Schema(description = "목표가 달성 종목 수", example = "3")
        private Long targetPriceReachedCount;
        
        @Schema(description = "손절가 도달 종목 수", example = "1")
        private Long stopLossReachedCount;
        
        @Schema(description = "가격 알림 활성화 종목 수", example = "12")
        private Long priceAlertEnabledCount;
        
        @Schema(description = "평균 수익률 (%)", example = "8.5")
        private BigDecimal averageChangePercentage;
        
        @Schema(description = "상승 종목 수", example = "10")
        private Long gainersCount;
        
        @Schema(description = "하락 종목 수", example = "5")
        private Long losersCount;
    }

    /**
     * 카테고리별 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리별 통계")
    public static class CategoryStat {
        
        @Schema(description = "카테고리명", example = "메이저 코인")
        private String category;
        
        @Schema(description = "종목 수", example = "5")
        private Long count;
        
        @Schema(description = "평균 변동률 (%)", example = "5.2")
        private BigDecimal averageChangePercentage;
    }

    /**
     * 가격 알림 설정 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "가격 알림 설정")
    public static class PriceAlertRequest {
        
        @NotNull(message = "가격 알림 활성화 여부는 필수입니다")
        @Schema(description = "가격 알림 활성화", example = "true")
        private Boolean priceAlertEnabled;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표가는 0보다 커야 합니다")
        @Schema(description = "목표가", example = "60000000")
        private BigDecimal targetPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "손절가는 0보다 커야 합니다")
        @Schema(description = "손절가", example = "45000000")
        private BigDecimal stopLossPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 상한가는 0보다 커야 합니다")
        @Schema(description = "목표 상한가", example = "70000000")
        private BigDecimal targetHighPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표 하한가는 0보다 커야 합니다")
        @Schema(description = "목표 하한가", example = "40000000")
        private BigDecimal targetLowPrice;
        
        @DecimalMin(value = "0.1", message = "변동률 알림은 0.1% 이상이어야 합니다")
        @DecimalMax(value = "50.0", message = "변동률 알림은 50% 이하여야 합니다")
        @Schema(description = "변동률 알림 임계값 (%)", example = "5.0")
        private BigDecimal changePercentageThreshold;
    }

    /**
     * 관심종목 필터 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관심종목 필터")
    public static class FilterRequest {
        
        @Schema(description = "카테고리", example = "메이저 코인")
        private String category;
        
        @Schema(description = "가격 알림 활성화 여부", example = "true")
        private Boolean priceAlertEnabled;
        
        @Schema(description = "목표가 달성 여부", example = "false")
        private Boolean targetPriceReached;
        
        @Schema(description = "손절가 도달 여부", example = "false")
        private Boolean stopLossPriceReached;
        
        @Schema(description = "최소 변동률 (%)", example = "-10.0")
        private BigDecimal minChangePercentage;
        
        @Schema(description = "최대 변동률 (%)", example = "20.0")
        private BigDecimal maxChangePercentage;
        
        @Schema(description = "정렬 기준", example = "CHANGE_PERCENTAGE_DESC", 
                allowableValues = {"CREATED_AT_DESC", "CHANGE_PERCENTAGE_DESC", "CHANGE_PERCENTAGE_ASC", "PRICE_DESC", "PRICE_ASC"})
        private String sortBy;
    }
}
