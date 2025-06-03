package com.coincommunity.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가격 알림 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceAlertDto {

    /**
     * 가격 알림 생성 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "가격 알림 생성 요청")
    public static class CreateRequest {
        @Schema(description = "코인 심볼", example = "BTC")
        @NotBlank(message = "코인 심볼은 필수입니다.")
        private String symbol;

        @Schema(description = "알림 타입", example = "ABOVE")
        @NotNull(message = "알림 타입은 필수입니다.")
        private AlertType alertType;

        @Schema(description = "목표 가격", example = "50000000")
        @NotNull(message = "목표 가격은 필수입니다.")
        @Positive(message = "목표 가격은 양수여야 합니다.")
        private BigDecimal targetPrice;

        @Schema(description = "알림 메시지", example = "비트코인이 5천만원을 돌파했습니다!")
        private String message;

        @Schema(description = "반복 여부", example = "false")
        @Builder.Default
        private boolean repeat = false;
    }

    /**
     * 가격 알림 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "가격 알림 응답")
    public static class Response {
        @Schema(description = "알림 ID")
        private Long id;

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "코인 심볼", example = "BTC")
        private String symbol;

        @Schema(description = "알림 타입", example = "ABOVE")
        private AlertType alertType;

        @Schema(description = "목표 가격", example = "50000000")
        private BigDecimal targetPrice;

        @Schema(description = "현재 가격", example = "48000000")
        private BigDecimal currentPrice;

        @Schema(description = "알림 메시지", example = "비트코인이 5천만원을 돌파했습니다!")
        private String message;

        @Schema(description = "알림 상태", example = "PENDING")
        private AlertStatus status;

        @Schema(description = "반복 여부", example = "false")
        private boolean repeat;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;

        @Schema(description = "마지막 트리거 시간")
        private LocalDateTime lastTriggeredAt;
    }

    /**
     * 가격 알림 타입
     */
    public enum AlertType {
        ABOVE("이상"),
        BELOW("이하"),
        PERCENT_CHANGE_UP("상승률"),
        PERCENT_CHANGE_DOWN("하락률");

        private final String displayName;

        AlertType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 가격 알림 상태
     */
    public enum AlertStatus {
        PENDING("대기중"),
        TRIGGERED("발동됨"),
        COMPLETED("완료됨"),
        CANCELLED("취소됨");

        private final String displayName;

        AlertStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
