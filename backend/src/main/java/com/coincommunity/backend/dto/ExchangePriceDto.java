package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래소별 시세 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "거래소별 시세 정보")
public class ExchangePriceDto {

    @Schema(description = "거래소명", example = "UPBIT")
    private String exchangeName;

    @Schema(description = "거래소 한글명", example = "업비트")
    private String exchangeKoreanName;

    @Schema(description = "거래소 타입", example = "DOMESTIC")
    private ExchangeType exchangeType;

    @Schema(description = "코인 심볼", example = "BTC")
    private String symbol;

    @Schema(description = "코인 한글명", example = "비트코인")
    private String koreanName;

    @Schema(description = "현재가 (KRW)", example = "45000000")
    private BigDecimal currentPrice;

    @Schema(description = "전일대비 변동가격", example = "1000000")
    private BigDecimal changePrice;

    @Schema(description = "전일대비 변동률 (%)", example = "2.27")
    private BigDecimal changeRate;

    @Schema(description = "24시간 최고가", example = "46000000")
    private BigDecimal highPrice24h;

    @Schema(description = "24시간 최저가", example = "43000000")
    private BigDecimal lowPrice24h;

    @Schema(description = "24시간 거래량", example = "1234.5678")
    private BigDecimal volume24h;

    @Schema(description = "24시간 거래대금 (KRW)", example = "55000000000")
    private BigDecimal tradeValue24h;

    @Schema(description = "매수호가", example = "44950000")
    private BigDecimal bidPrice;

    @Schema(description = "매도호가", example = "45050000")
    private BigDecimal askPrice;

    @Schema(description = "스프레드 (매도-매수)", example = "100000")
    private BigDecimal spread;

    @Schema(description = "스프레드 비율 (%)", example = "0.22")
    private BigDecimal spreadRate;

    @Schema(description = "거래 상태", example = "NORMAL")
    private TradingStatus status;

    @Schema(description = "시장 경고", example = "NONE")
    private MarketWarning marketWarning;

    @Schema(description = "마지막 업데이트 시간")
    private LocalDateTime lastUpdated;

    @Schema(description = "가격 신뢰도 (1-100)", example = "95")
    private Integer priceReliability;

    @Schema(description = "API 응답 시간 (ms)", example = "150")
    private Long responseTime;

    public enum ExchangeType {
        DOMESTIC("국내"),
        FOREIGN("해외");

        private final String koreanName;

        ExchangeType(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    public enum TradingStatus {
        NORMAL("정상거래"),
        SUSPENDED("거래중단"),
        DELISTED("상장폐지"),
        MAINTENANCE("점검중"),
        CAUTION("투자유의");

        private final String koreanName;

        TradingStatus(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    public enum MarketWarning {
        NONE("없음"),
        CAUTION("투자유의"),
        WARNING("투자경고"),
        DANGER("투자위험");

        private final String koreanName;

        MarketWarning(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }
}
