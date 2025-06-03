package com.coincommunity.backend.external.exchange.upbit;

import com.coincommunity.backend.entity.CoinPrice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 업비트 API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://docs.upbit.com/reference
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitApiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.upbit.base-url:https://api.upbit.com/v1}")
    private String baseUrl;
    
    private static final String EXCHANGE = "UPBIT";
    
    /**
     * 업비트에서 제공하는 시장 코드(마켓) 목록을 조회합니다.
     */
    public List<String> getMarkets() {
        try {
            String url = baseUrl + "/market/all";
            UpbitMarket[] markets = restTemplate.getForObject(url, UpbitMarket[].class);
            
            if (markets != null) {
                return Arrays.stream(markets)
                        .filter(m -> m.getMarket().startsWith("KRW-"))
                        .map(UpbitMarket::getMarket)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("업비트 마켓 정보를 가져오는 중 오류가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 주어진 마켓 코드에 대한 현재 시세 정보를 조회합니다.
     */
    public List<CoinPrice> getTickers(List<String> markets) {
        if (markets == null || markets.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String marketParam = String.join(",", markets);
            String url = baseUrl + "/ticker?markets=" + marketParam;
            
            UpbitTicker[] tickers = restTemplate.getForObject(url, UpbitTicker[].class);
            
            if (tickers != null) {
                List<CoinPrice> coinPrices = new ArrayList<>();
                
                for (UpbitTicker ticker : tickers) {
                    try {
                        // 마켓 코드에서 코인 ID 추출 (ex: KRW-BTC -> BTC)
                        String[] marketParts = ticker.getMarket().split("-");
                        if (marketParts.length == 2) {
                            String coinId = marketParts[1];
                            
                            CoinPrice coinPrice = new CoinPrice();
                            coinPrice.setCoinId(coinId);
                            
                            // 코인 이름은 별도로 설정 필요
                            Map<String, String> coinNames = getCoinNames();
                            String koreanName = coinNames.getOrDefault(coinId, coinId);
                            coinPrice.setKoreanName(koreanName);
                            coinPrice.setEnglishName(coinId);
                            
                            coinPrice.setCurrentPrice(ticker.getTradePrice());
                            coinPrice.setPriceChangePercent(ticker.getSignedChangeRate().multiply(BigDecimal.valueOf(100)));
                            coinPrice.setVolume24h(ticker.getAccTradeVolume24h());
                            coinPrice.setExchange(EXCHANGE);
                            coinPrice.setHighPrice24h(ticker.getHighPrice());
                            coinPrice.setLowPrice24h(ticker.getLowPrice());
                            coinPrice.setMarketCap(null); // 업비트는 시가총액 제공 안함
                            coinPrice.setLastUpdated(LocalDateTime.now());
                            
                            coinPrices.add(coinPrice);
                        }
                    } catch (Exception e) {
                        log.warn("업비트 티커를 처리하는 중 오류가 발생했습니다: {}", ticker.getMarket(), e);
                    }
                }
                
                return coinPrices;
            }
        } catch (Exception e) {
            log.error("업비트 티커 정보를 가져오는 중 오류가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 코인 ID와 한글 이름 매핑을 반환합니다.
     */
    private Map<String, String> getCoinNames() {
        try {
            String url = baseUrl + "/market/all";
            UpbitMarket[] markets = restTemplate.getForObject(url, UpbitMarket[].class);
            
            if (markets != null) {
                return Arrays.stream(markets)
                        .filter(m -> m.getMarket().startsWith("KRW-"))
                        .collect(Collectors.toMap(
                            m -> m.getMarket().split("-")[1],
                            UpbitMarket::getKoreanName,
                            (existing, replacement) -> existing
                        ));
            }
        } catch (Exception e) {
            log.error("업비트 코인 이름 정보를 가져오는 중 오류가 발생했습니다", e);
        }
        
        return Map.of();
    }
    
    /**
     * 주요 코인들의 현재 시세 정보를 조회합니다.
     */
    public List<CoinPrice> getTopCoinPrices() {
        // 주요 코인 목록
        List<String> topCoins = List.of("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-SOL", 
                "KRW-ADA", "KRW-DOGE", "KRW-DOT", "KRW-MATIC", "KRW-AVAX", "KRW-TRX");
        
        return getTickers(topCoins);
    }
}
