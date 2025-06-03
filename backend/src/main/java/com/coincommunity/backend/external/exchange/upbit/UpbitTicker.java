package com.coincommunity.backend.external.exchange.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 업비트 현재가 정보 응답을 위한 DTO
 */
@Getter
@Setter
public class UpbitTicker {
    
    /**
     * 마켓 코드 (예: KRW-BTC)
     */
    private String market;
    
    /**
     * 현재가
     */
    @JsonProperty("trade_price")
    private BigDecimal tradePrice;
    
    /**
     * 전일 대비 변화액
     */
    @JsonProperty("change_price")
    private BigDecimal changePrice;
    
    /**
     * 전일 대비 변화율
     */
    @JsonProperty("change_rate")
    private BigDecimal changeRate;
    
    /**
     * 부호가 있는 전일 대비 변화율
     */
    @JsonProperty("signed_change_rate")
    private BigDecimal signedChangeRate;
    
    /**
     * 24시간 누적 거래량
     */
    @JsonProperty("acc_trade_volume_24h")
    private BigDecimal accTradeVolume24h;
    
    /**
     * 24시간 누적 거래대금
     */
    @JsonProperty("acc_trade_price_24h")
    private BigDecimal accTradePrice24h;
    
    /**
     * 최고가
     */
    @JsonProperty("high_price")
    private BigDecimal highPrice;
    
    /**
     * 최저가
     */
    @JsonProperty("low_price")
    private BigDecimal lowPrice;
    
    /**
     * 시가
     */
    @JsonProperty("opening_price")
    private BigDecimal openingPrice;
    
    /**
     * 전일 종가
     */
    @JsonProperty("prev_closing_price")
    private BigDecimal prevClosingPrice;
    
    /**
     * 52주 신고가
     */
    @JsonProperty("highest_52_week_price")
    private BigDecimal highest52WeekPrice;
    
    /**
     * 52주 최저가
     */
    @JsonProperty("lowest_52_week_price")
    private BigDecimal lowest52WeekPrice;
}
