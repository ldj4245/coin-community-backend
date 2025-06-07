package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.Transaction;
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
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 거래 내역 관련 DTO 클래스
 * 암호화폐 매수/매도 거래 정보를 위한 데이터 전송 객체
 */
public class TransactionDto {

    /**
     * 거래 내역 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래 내역 생성 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CreateRequest {
        
        @NotNull(message = "포트폴리오 ID는 필수입니다")
        @Schema(description = "포트폴리오 ID", example = "1")
        private Long portfolioId;
        
        @NotBlank(message = "코인 심볼은 필수입니다")
        @Size(max = 20, message = "코인 심볼은 20자를 초과할 수 없습니다")
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @NotNull(message = "거래 유형은 필수입니다")
        @Schema(description = "거래 유형", example = "BUY", allowableValues = {"BUY", "SELL"})
        private Transaction.TransactionType transactionType;
        
        @NotNull(message = "수량은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "수량은 0보다 커야 합니다")
        @Schema(description = "거래 수량", example = "0.1")
        private BigDecimal quantity;
        
        @NotNull(message = "가격은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다")
        @Schema(description = "거래 가격", example = "50000000")
        private BigDecimal price;
        
        @DecimalMin(value = "0.0", message = "수수료는 0 이상이어야 합니다")
        @Schema(description = "거래 수수료", example = "10000")
        private BigDecimal fee;
        
        @Size(max = 50, message = "거래소명은 50자를 초과할 수 없습니다")
        @Schema(description = "거래소명", example = "Upbit")
        private String exchange;
        
        @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다")
        @Schema(description = "거래 메모", example = "분할 매수")
        private String notes;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "거래 일시 (선택사항, 기본값: 현재 시간)")
        private LocalDateTime transactionDate;
    }

    /**
     * 거래 내역 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "거래 내역 응답")
    public static class Response {
        
        @Schema(description = "거래 ID", example = "1")
        private Long id;
        
        @Schema(description = "포트폴리오 ID", example = "1")
        private Long portfolioId;
        
        @Schema(description = "포트폴리오 이름", example = "메인 포트폴리오")
        private String portfolioName;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "코인 이름", example = "Bitcoin")
        private String coinName;
        
        @Schema(description = "거래 유형", example = "BUY")
        private Transaction.TransactionType transactionType;
        
        @Schema(description = "거래 수량", example = "0.1")
        private BigDecimal quantity;
        
        @Schema(description = "거래 가격", example = "50000000")
        private BigDecimal price;
        
        @Schema(description = "거래 금액", example = "5000000")
        private BigDecimal totalAmount;
        
        @Schema(description = "거래 수수료", example = "10000")
        private BigDecimal fee;
        
        @Schema(description = "실제 거래 금액 (수수료 포함)", example = "5010000")
        private BigDecimal netAmount;
        
        @Schema(description = "거래소명", example = "Upbit")
        private String exchange;
        
        @Schema(description = "거래 메모")
        private String notes;
        
        @Schema(description = "실현 손익 (매도 시)", example = "500000")
        private BigDecimal realizedPnl;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "거래 일시")
        private LocalDateTime transactionDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
    }

    /**
     * 거래 내역 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래 내역 요약")
    public static class Summary {
        
        @Schema(description = "총 거래 건수", example = "25")
        private Long totalTransactions;
        
        @Schema(description = "매수 거래 건수", example = "15")
        private Long buyTransactions;
        
        @Schema(description = "매도 거래 건수", example = "10")
        private Long sellTransactions;
        
        @Schema(description = "총 매수 금액", example = "10000000")
        private BigDecimal totalBuyAmount;
        
        @Schema(description = "총 매도 금액", example = "12000000")
        private BigDecimal totalSellAmount;
        
        @Schema(description = "총 실현 손익", example = "1500000")
        private BigDecimal totalRealizedPnl;
        
        @Schema(description = "총 수수료", example = "250000")
        private BigDecimal totalFees;
        
        @Schema(description = "가장 많이 거래한 코인", example = "BTC")
        private String mostTradedCoin;
        
        @Schema(description = "최근 거래일", example = "2024-01-15T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastTransactionDate;
    }

    /**
     * 거래 내역 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래 내역 통계")
    public static class Statistics {
        
        @Schema(description = "기간", example = "30일")
        private String period;
        
        @Schema(description = "거래 건수", example = "12")
        private Long transactionCount;
        
        @Schema(description = "거래 금액", example = "5000000")
        private BigDecimal totalVolume;
        
        @Schema(description = "평균 거래 금액", example = "416667")
        private BigDecimal averageTransactionAmount;
        
        @Schema(description = "실현 손익", example = "500000")
        private BigDecimal realizedPnl;
        
        @Schema(description = "수익률 (%)", example = "12.5")
        private BigDecimal returnPercentage;
        
        @Schema(description = "승률 (%)", example = "75.0")
        private BigDecimal winRate;
    }

    /**
     * 거래 필터 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래 내역 필터")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class FilterRequest {
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "거래 유형", example = "BUY")
        private Transaction.TransactionType transactionType;
        
        @Schema(description = "거래소명", example = "Upbit")
        private String exchange;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "시작일", example = "2024-01-01")
        private LocalDateTime startDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "종료일", example = "2024-01-31")
        private LocalDateTime endDate;
        
        @Schema(description = "최소 거래 금액", example = "100000")
        private BigDecimal minAmount;
        
        @Schema(description = "최대 거래 금액", example = "10000000")
        private BigDecimal maxAmount;
    }
}
