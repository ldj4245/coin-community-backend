package com.coincommunity.backend.external.exchange;

import com.coincommunity.backend.dto.ExchangePriceDto;
import java.util.List;

/**
 * 거래소 API 전략 인터페이스
 * 각 거래소별 API 클라이언트가 구현해야 하는 공통 인터페이스
 */
public interface ExchangeApiStrategy {
    
    /**
     * 거래소 이름 반환
     */
    String getExchangeName();
    
    /**
     * 거래소 타입 반환 (국내/해외)
     */
    ExchangeType getExchangeType();
    
    /**
     * 지원하는 코인 목록 조회
     */
    List<String> getSupportedCoins();
    
    /**
     * 모든 코인 가격 정보 조회
     */
    List<ExchangePriceDto> getAllCoinPrices();
    
    /**
     * 특정 코인 가격 정보 조회
     */
    ExchangePriceDto getCoinPrice(String coinSymbol);
    
    /**
     * 시가총액 상위 코인 조회
     */
    List<ExchangePriceDto> getTopCoinsByMarketCap(int limit);
    
    /**
     * API 연결 상태 확인
     */
    boolean isHealthy();
    
    /**
     * 거래소 타입 열거형
     */
    enum ExchangeType {
        DOMESTIC("국내"),
        FOREIGN("해외");
        
        private final String description;
        
        ExchangeType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
