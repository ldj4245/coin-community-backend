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
import java.util.List;

/**
 * 포트폴리오 관련 DTO 클래스
 * 사용자의 암호화폐 포트폴리오 정보를 전송하기 위한 데이터 전송 객체
 */
public class PortfolioDto {

    /**
     * 포트폴리오 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 생성 요청")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CreateRequest {
        
        @NotBlank(message = "포트폴리오 이름은 필수입니다")
        @Size(max = 100, message = "포트폴리오 이름은 100자를 초과할 수 없습니다")
        @Schema(description = "포트폴리오 이름", example = "메인 투자 포트폴리오")
        private String name;
        
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        @Schema(description = "포트폴리오 설명", example = "장기 투자용 포트폴리오")
        private String description;
        
        @NotNull(message = "투자 금액은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "투자 금액은 0보다 커야 합니다")
        @Schema(description = "초기 투자 금액", example = "1000000")
        private BigDecimal initialInvestment;
        
        @NotNull(message = "공개 여부는 필수입니다")
        @Schema(description = "공개 여부", example = "true")
        private Boolean isPublic;
    }

    /**
     * 포트폴리오 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 업데이트 요청")
    public static class UpdateRequest {
        
        @Size(max = 100, message = "포트폴리오 이름은 100자를 초과할 수 없습니다")
        @Schema(description = "포트폴리오 이름", example = "수정된 포트폴리오")
        private String name;
        
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
        @Schema(description = "포트폴리오 설명")
        private String description;
        
        @Schema(description = "공개 여부")
        private Boolean isPublic;
    }

    /**
     * 포트폴리오 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "포트폴리오 응답")
    public static class Response {
        
        @Schema(description = "포트폴리오 ID", example = "1")
        private Long id;
        
        @Schema(description = "포트폴리오 이름", example = "메인 투자 포트폴리오")
        private String name;
        
        @Schema(description = "포트폴리오 설명")
        private String description;
        
        @Schema(description = "초기 투자 금액", example = "1000000")
        private BigDecimal initialInvestment;
        
        @Schema(description = "현재 총 가치", example = "1250000")
        private BigDecimal currentValue;
        
        @Schema(description = "총 손익", example = "250000")
        private BigDecimal totalProfitLoss;
        
        @Schema(description = "총 수익률 (%)", example = "25.0")
        private BigDecimal totalReturnPercentage;
        
        @Schema(description = "공개 여부", example = "true")
        private Boolean isPublic;
        
        @Schema(description = "포트폴리오 아이템 수", example = "5")
        private Integer itemCount;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;
        
        @Schema(description = "포트폴리오 아이템 목록")
        private List<PortfolioItemDto.Response> items;
    }

    /**
     * 포트폴리오 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 요약")
    public static class Summary {
        
        @Schema(description = "포트폴리오 ID", example = "1")
        private Long id;
        
        @Schema(description = "포트폴리오 이름", example = "메인 투자 포트폴리오")
        private String name;
        
        @Schema(description = "현재 총 가치", example = "1250000")
        private BigDecimal currentValue;
        
        @Schema(description = "총 수익률 (%)", example = "25.0")
        private BigDecimal totalReturnPercentage;
        
        @Schema(description = "24시간 변동률 (%)", example = "3.5")
        private BigDecimal dailyChangePercentage;
        
        @Schema(description = "포트폴리오 아이템 수", example = "5")
        private Integer itemCount;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "마지막 업데이트")
        private LocalDateTime lastUpdated;
    }

    /**
     * 포트폴리오 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포트폴리오 통계")
    public static class Statistics {
        
        @Schema(description = "총 포트폴리오 수", example = "3")
        private Long totalPortfolios;
        
        @Schema(description = "총 투자 금액", example = "5000000")
        private BigDecimal totalInvestment;
        
        @Schema(description = "총 현재 가치", example = "6250000")
        private BigDecimal totalCurrentValue;
        
        @Schema(description = "총 손익", example = "1250000")
        private BigDecimal totalProfitLoss;
        
        @Schema(description = "평균 수익률 (%)", example = "25.0")
        private BigDecimal averageReturnPercentage;
        
        @Schema(description = "최고 수익률 (%)", example = "45.0")
        private BigDecimal bestReturnPercentage;
        
        @Schema(description = "최저 수익률 (%)", example = "5.0")
        private BigDecimal worstReturnPercentage;
    }
}
