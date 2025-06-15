package com.coincommunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포트폴리오 아이템 관련 DTO 클래스
 * 포트폴리오에 포함된 개별 코인 정보를 전송하기 위한 데이터 전송 객체
 */
public class PortfolioItemDto {

    /**
     * 포트폴리오 아이템 추가 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 아이템 추가 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AddRequest {
        
        @NotBlank(message = "코인 심볼은 필수입니다")
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @NotNull(message = "수량은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "수량은 0보다 커야 합니다")
        @Schema(description = "보유 수량", example = "0.5")
        private BigDecimal quantity;
        
        @NotNull(message = "평균 매수가는 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "평균 매수가는 0보다 커야 합니다")
        @Schema(description = "평균 매수가", example = "50000000")
        private BigDecimal averagePrice;
        
        @Size(max = 500, message = "메모는 500자를 초과할 수 없습니다")
        @Schema(description = "메모", example = "장기 투자용")
        private String notes;
    }

    /**
     * 포트폴리오 아이템 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 아이템 업데이트 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class UpdateRequest {
        
        @DecimalMin(value = "0.0", inclusive = false, message = "수량은 0보다 커야 합니다")
        @Schema(description = "보유 수량", example = "0.75")
        private BigDecimal quantity;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "평균 매수가는 0보다 커야 합니다")
        @Schema(description = "평균 매수가", example = "48000000")
        private BigDecimal averagePrice;
        
        @Size(max = 500, message = "메모는 500자를 초과할 수 없습니다")
        @Schema(description = "메모", example = "추가 매수 후 업데이트")
        private String notes;
    }

    /**
     * 포트폴리오 아이템 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "포트폴리오 아이템 응답")
    public static class Response {
        
        @Schema(description = "아이템 ID", example = "1")
        private Long id;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "코인 이름", example = "Bitcoin")
        private String coinName;
        
        @Schema(description = "보유 수량", example = "0.5")
        private BigDecimal quantity;
        
        @Schema(description = "평균 매수가", example = "50000000")
        private BigDecimal averagePrice;
        
        @Schema(description = "현재 가격", example = "55000000")
        private BigDecimal currentPrice;
        
        @Schema(description = "총 매수 금액", example = "25000000")
        private BigDecimal totalCost;
        
        @Schema(description = "현재 평가 금액", example = "27500000")
        private BigDecimal currentValue;
        
        @Schema(description = "손익", example = "2500000")
        private BigDecimal profitLoss;
        
        @Schema(description = "수익률 (%)", example = "10.0")
        private BigDecimal returnPercentage;
        
        @Schema(description = "메모", example = "장기 투자용")
        private String notes;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "가격 업데이트 시점")
        private LocalDateTime priceUpdatedAt;
    }

    /**
     * 포트폴리오 아이템 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 아이템 요약")
    public static class Summary {
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "현재 가치", example = "27500000")
        private BigDecimal currentValue;
        
        @Schema(description = "수익률 (%)", example = "10.0")
        private BigDecimal returnPercentage;
        
        @Schema(description = "포트폴리오 내 비중 (%)", example = "45.5")
        private BigDecimal weightPercentage;
    }
}
