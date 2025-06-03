package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.CoinAnalysis;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 코인 분석 관련 DTO 클래스
 * 사용자의 암호화폐 분석 및 예측 정보를 위한 데이터 전송 객체
 */
public class CoinAnalysisDto {

    /**
     * 코인 분석 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코인 분석 생성 요청")
    public static class CreateRequest {
        
        @NotBlank(message = "코인 심볼은 필수입니다")
        @Size(max = 20, message = "코인 심볼은 20자를 초과할 수 없습니다")
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        @Schema(description = "분석 제목", example = "비트코인 Q1 기술적 분석")
        private String title;
        
        @NotBlank(message = "분석 내용은 필수입니다")
        @Size(max = 10000, message = "분석 내용은 10000자를 초과할 수 없습니다")
        @Schema(description = "분석 내용")
        private String content;
        
        @NotNull(message = "분석 유형은 필수입니다")
        @Schema(description = "분석 유형", example = "TECHNICAL", allowableValues = {"TECHNICAL", "FUNDAMENTAL", "SENTIMENT", "NEWS_BASED"})
        private CoinAnalysis.AnalysisType analysisType;
        
        @NotNull(message = "예상 방향은 필수입니다")
        @Schema(description = "예상 방향", example = "BULLISH", allowableValues = {"BULLISH", "BEARISH", "NEUTRAL"})
        private CoinAnalysis.PredictionDirection predictionDirection;
        
        @NotNull(message = "예측 기간은 필수입니다")
        @Schema(description = "예측 기간", example = "SHORT_TERM", allowableValues = {"SHORT_TERM", "MEDIUM_TERM", "LONG_TERM"})
        private CoinAnalysis.TimeFrame timeFrame;
        
        @Min(value = 1, message = "신뢰도는 1 이상이어야 합니다")
        @Max(value = 10, message = "신뢰도는 10 이하여야 합니다")
        @Schema(description = "신뢰도 (1-10)", example = "7")
        private Integer confidenceLevel;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표가는 0보다 커야 합니다")
        @Schema(description = "목표가", example = "65000000")
        private BigDecimal targetPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "손절가는 0보다 커야 합니다")
        @Schema(description = "손절가", example = "48000000")
        private BigDecimal stopLossPrice;
        
        @Future(message = "예측 만료일은 미래여야 합니다")
        @Schema(description = "예측 만료일", example = "2024-03-31")
        private LocalDate predictionExpiryDate;
        
        @Schema(description = "태그 목록", example = "[\"기술적분석\", \"RSI\", \"이동평균\"]")
        private List<String> tags;
    }

    /**
     * 코인 분석 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코인 분석 업데이트 요청")
    public static class UpdateRequest {
        
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        @Schema(description = "분석 제목")
        private String title;
        
        @Size(max = 10000, message = "분석 내용은 10000자를 초과할 수 없습니다")
        @Schema(description = "분석 내용")
        private String content;
        
        @Min(value = 1, message = "신뢰도는 1 이상이어야 합니다")
        @Max(value = 10, message = "신뢰도는 10 이하여야 합니다")
        @Schema(description = "신뢰도 (1-10)")
        private Integer confidenceLevel;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "목표가는 0보다 커야 합니다")
        @Schema(description = "목표가")
        private BigDecimal targetPrice;
        
        @DecimalMin(value = "0.0", inclusive = false, message = "손절가는 0보다 커야 합니다")
        @Schema(description = "손절가")
        private BigDecimal stopLossPrice;
        
        @Schema(description = "태그 목록")
        private List<String> tags;
    }

    /**
     * 코인 분석 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "코인 분석 응답")
    public static class Response {
        
        @Schema(description = "분석 ID", example = "1")
        private Long id;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "코인 이름", example = "Bitcoin")
        private String coinName;
        
        @Schema(description = "분석 제목", example = "비트코인 Q1 기술적 분석")
        private String title;
        
        @Schema(description = "분석 내용")
        private String content;
        
        @Schema(description = "분석 유형", example = "TECHNICAL")
        private CoinAnalysis.AnalysisType analysisType;
        
        @Schema(description = "예상 방향", example = "BULLISH")
        private CoinAnalysis.PredictionDirection predictionDirection;
        
        @Schema(description = "예측 기간", example = "SHORT_TERM")
        private CoinAnalysis.TimeFrame timeFrame;
        
        @Schema(description = "신뢰도 (1-10)", example = "7")
        private Integer confidenceLevel;
        
        @Schema(description = "분석 시점 가격", example = "52000000")
        private BigDecimal analysisPriceAtTime;
        
        @Schema(description = "현재가", example = "55000000")
        private BigDecimal currentPrice;
        
        @Schema(description = "목표가", example = "65000000")
        private BigDecimal targetPrice;
        
        @Schema(description = "손절가", example = "48000000")
        private BigDecimal stopLossPrice;
        
        @Schema(description = "예측 상태", example = "ACTIVE")
        private CoinAnalysis.PredictionStatus predictionStatus;
        
        @Schema(description = "예측 정확도 (%)", example = "75.5")
        private BigDecimal accuracyScore;
        
        @Schema(description = "조회수", example = "150")
        private Long viewCount;
        
        @Schema(description = "좋아요 수", example = "25")
        private Long likeCount;
        
        @Schema(description = "댓글 수", example = "8")
        private Long commentCount;
        
        @Schema(description = "작성자 정보")
        private UserDto.Summary author;
        
        @Schema(description = "태그 목록")
        private List<String> tags;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "예측 만료일")
        private LocalDate predictionExpiryDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;
    }

    /**
     * 코인 분석 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코인 분석 요약")
    public static class Summary {
        
        @Schema(description = "분석 ID", example = "1")
        private Long id;
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "분석 제목", example = "비트코인 Q1 기술적 분석")
        private String title;
        
        @Schema(description = "분석 유형", example = "TECHNICAL")
        private CoinAnalysis.AnalysisType analysisType;
        
        @Schema(description = "예상 방향", example = "BULLISH")
        private CoinAnalysis.PredictionDirection predictionDirection;
        
        @Schema(description = "신뢰도 (1-10)", example = "7")
        private Integer confidenceLevel;
        
        @Schema(description = "예측 정확도 (%)", example = "75.5")
        private BigDecimal accuracyScore;
        
        @Schema(description = "좋아요 수", example = "25")
        private Long likeCount;
        
        @Schema(description = "작성자명", example = "분석전문가")
        private String authorNickname;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
    }

    /**
     * 코인 분석 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코인 분석 통계")
    public static class Statistics {
        
        @Schema(description = "총 분석 수", example = "50")
        private Long totalAnalyses;
        
        @Schema(description = "예측 정확도 평균 (%)", example = "72.5")
        private BigDecimal averageAccuracy;
        
        @Schema(description = "성공한 예측 수", example = "30")
        private Long successfulPredictions;
        
        @Schema(description = "실패한 예측 수", example = "15")
        private Long failedPredictions;
        
        @Schema(description = "진행 중인 예측 수", example = "5")
        private Long activePredictions;
        
        @Schema(description = "분석 유형별 통계")
        private List<AnalysisTypeStat> analysisTypeStats;
        
        @Schema(description = "예측 방향별 정확도")
        private List<PredictionDirectionStat> predictionDirectionStats;
        
        @Schema(description = "시간 프레임별 정확도")
        private List<TimeFrameStat> timeFrameStats;
    }

    /**
     * 분석 유형별 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "분석 유형별 통계")
    public static class AnalysisTypeStat {
        
        @Schema(description = "분석 유형", example = "TECHNICAL")
        private CoinAnalysis.AnalysisType analysisType;
        
        @Schema(description = "분석 수", example = "20")
        private Long count;
        
        @Schema(description = "평균 정확도 (%)", example = "75.0")
        private BigDecimal averageAccuracy;
    }

    /**
     * 예측 방향별 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "예측 방향별 통계")
    public static class PredictionDirectionStat {
        
        @Schema(description = "예측 방향", example = "BULLISH")
        private CoinAnalysis.PredictionDirection predictionDirection;
        
        @Schema(description = "예측 수", example = "25")
        private Long count;
        
        @Schema(description = "성공률 (%)", example = "70.0")
        private BigDecimal successRate;
    }

    /**
     * 시간 프레임별 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "시간 프레임별 통계")
    public static class TimeFrameStat {
        
        @Schema(description = "시간 프레임", example = "SHORT_TERM")
        private CoinAnalysis.TimeFrame timeFrame;
        
        @Schema(description = "예측 수", example = "15")
        private Long count;
        
        @Schema(description = "평균 정확도 (%)", example = "68.0")
        private BigDecimal averageAccuracy;
    }

    /**
     * 코인 분석 필터 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코인 분석 필터")
    public static class FilterRequest {
        
        @Schema(description = "코인 심볼", example = "BTC")
        private String coinSymbol;
        
        @Schema(description = "분석 유형", example = "TECHNICAL")
        private CoinAnalysis.AnalysisType analysisType;
        
        @Schema(description = "예측 방향", example = "BULLISH")
        private CoinAnalysis.PredictionDirection predictionDirection;
        
        @Schema(description = "시간 프레임", example = "SHORT_TERM")
        private CoinAnalysis.TimeFrame timeFrame;
        
        @Schema(description = "예측 상태", example = "ACTIVE")
        private CoinAnalysis.PredictionStatus predictionStatus;
        
        @Schema(description = "최소 신뢰도", example = "5")
        private Integer minConfidenceLevel;
        
        @Schema(description = "최소 정확도 (%)", example = "60.0")
        private BigDecimal minAccuracyScore;
        
        @Schema(description = "태그", example = "기술적분석")
        private String tag;
        
        @Schema(description = "작성자 ID", example = "1")
        private Long authorId;
        
        @Schema(description = "정렬 기준", example = "CREATED_AT_DESC",
                allowableValues = {"CREATED_AT_DESC", "ACCURACY_DESC", "LIKE_COUNT_DESC", "VIEW_COUNT_DESC", "CONFIDENCE_DESC"})
        private String sortBy;
    }
    
    /**
     * 예측 정확도 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "예측 정확도 업데이트 요청")
    public static class UpdateAccuracyRequest {
        
        @NotNull(message = "실제 가격은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "실제 가격은 0보다 커야 합니다")
        @Schema(description = "실제 가격", example = "50000.00")
        private BigDecimal actualPrice;
    }
}
