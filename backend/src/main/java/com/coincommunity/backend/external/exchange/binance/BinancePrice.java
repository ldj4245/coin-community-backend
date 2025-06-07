package com.coincommunity.backend.external.exchange.binance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Binance API 티커 정보 응답 모델
 */
@Data
public class BinancePrice {
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("priceChange")
    private BigDecimal priceChange;
    
    @JsonProperty("priceChangePercent")
    private BigDecimal priceChangePercent;
    
    @JsonProperty("volume")
    private BigDecimal volume;
    
    @JsonProperty("count")
    private Long count;
    
    @JsonProperty("highPrice")
    private BigDecimal highPrice;
    
    @JsonProperty("lowPrice")
    private BigDecimal lowPrice;
} 