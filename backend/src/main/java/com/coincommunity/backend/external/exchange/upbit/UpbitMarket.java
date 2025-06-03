package com.coincommunity.backend.external.exchange.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 업비트 마켓 정보 응답을 위한 DTO
 */
@Getter
@Setter
public class UpbitMarket {
    
    /**
     * 마켓 코드 (예: KRW-BTC)
     */
    private String market;
    
    /**
     * 한글 이름
     */
    @JsonProperty("korean_name")
    private String koreanName;
    
    /**
     * 영어 이름
     */
    @JsonProperty("english_name")
    private String englishName;
    
    /**
     * 마켓 경고 여부
     */
    @JsonProperty("market_warning")
    private String marketWarning;
}
