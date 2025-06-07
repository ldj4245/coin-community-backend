package com.coincommunity.backend.external.exchange.upbit;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.dto.ExchangePriceDto.TradingStatus;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 업비트 API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://docs.upbit.com/reference
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitApiClient implements ExchangeApiStrategy {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.upbit.base-url}")
    private String baseUrl;
    
    @Value("${external.upbit.version}")
    private String version;
    
    private static final String EXCHANGE = "UPBIT";
    
    // API 엔드포인트 상수 정의
    private static final String MARKET_ALL_ENDPOINT = "/market/all";
    private static final String TICKER_ENDPOINT = "/ticker";
    
    private static final Map<String, String> coinNameCache = new ConcurrentHashMap<>();
    private static LocalDateTime lastCacheRefresh = LocalDateTime.now().minusDays(1);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 완전한 API URL 생성
     */
    private String buildUrl(String endpoint) {
        return baseUrl + "/" + version + endpoint;
    }
    
    @Override
    public String getExchangeName() {
        return EXCHANGE;
    }
    
    @Override
    public ExchangeApiStrategy.ExchangeType getExchangeType() {
        return ExchangeApiStrategy.ExchangeType.DOMESTIC;
    }
    
    @Override
    public List<String> getSupportedCoins() {
        return getMarkets().stream()
                .map(market -> market.split("-")[1])
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ExchangePriceDto> getAllCoinPrices() {
        List<String> markets = getMarkets();
        return getTickers(markets);
    }
    
    @Override
    public ExchangePriceDto getCoinPrice(String coinSymbol) {
        String market = "KRW-" + coinSymbol.toUpperCase();
        List<ExchangePriceDto> prices = getTickers(List.of(market));
        return prices.isEmpty() ? null : prices.get(0);
    }
    
    @Override
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        return getTopCoinPrices().stream().limit(limit).collect(Collectors.toList());
    }
    
    @Override
    public boolean isHealthy() {
        try {
            String url = buildUrl(MARKET_ALL_ENDPOINT);
            UpbitMarket[] markets = restTemplate.getForObject(url, UpbitMarket[].class);
            return markets != null && markets.length > 0;
        } catch (Exception e) {
            log.warn("업비트 API 건강 상태 확인 실패", e);
            return false;
        }
    }
    
    /**
     * 업비트에서 제공하는 시장 코드(마켓) 목록을 조회합니다.
     */
    public List<String> getMarkets() {
        try {
            String url = buildUrl(MARKET_ALL_ENDPOINT);
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
    public List<ExchangePriceDto> getTickers(List<String> markets) {
        if (markets == null || markets.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String marketParam = String.join(",", markets);
            String url = buildUrl(TICKER_ENDPOINT + "?markets=" + marketParam);
            
            UpbitTicker[] tickers = restTemplate.getForObject(url, UpbitTicker[].class);
            
            if (tickers != null) {
                List<ExchangePriceDto> exchangePrices = new ArrayList<>();
                
                // 코인 이름 정보를 한 번만 가져오도록 개선
                Map<String, String> coinNames = getCoinNames();

                for (UpbitTicker ticker : tickers) {
                    try {
                        // 마켓 코드에서 코인 ID 추출 (ex: KRW-BTC -> BTC)
                        String[] marketParts = ticker.getMarket().split("-");
                        if (marketParts.length == 2) {
                            String coinId = marketParts[1];
                            
                            // 미리 가져온 코인 이름 맵에서 조회
                            String koreanName = coinNames.getOrDefault(coinId, coinId);
                            
                            ExchangePriceDto exchangePrice = ExchangePriceDto.builder()
                                .exchangeName(EXCHANGE)
                                .exchangeKoreanName("업비트")
                                .exchangeType(com.coincommunity.backend.dto.ExchangePriceDto.ExchangeType.DOMESTIC)
                                .symbol(coinId)
                                .koreanName(koreanName)
                                .currentPrice(ticker.getTradePrice())
                                .changeRate(ticker.getSignedChangeRate().multiply(BigDecimal.valueOf(100)))
                                .highPrice24h(ticker.getHighPrice())
                                .lowPrice24h(ticker.getLowPrice())
                                .volume24h(ticker.getAccTradeVolume24h())
                                .status(TradingStatus.NORMAL)
                                .lastUpdated(LocalDateTime.now())
                                .build();
                            
                            exchangePrices.add(exchangePrice);
                        }
                    } catch (Exception e) {
                        log.warn("업비트 티커를 처리하는 중 오류가 발생했습니다: {}", ticker.getMarket(), e);
                    }
                }
                
                return exchangePrices;
            }
        } catch (Exception e) {
            log.error("업비트 티커 정보를 가져오는 중 오류가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 코인 ID와 한글 이름 매핑을 반환합니다.
     * 메모리 캐싱을 활용해 API 호출 횟수를 줄임
     */
    private Map<String, String> getCoinNames() {
        // 캐시가 비어있거나 TTL이 만료된 경우에만 API 호출
        if (coinNameCache.isEmpty() || Duration.between(lastCacheRefresh, LocalDateTime.now()).compareTo(CACHE_TTL) > 0) {
            try {
                log.info("업비트 코인 이름 정보 캐시를 갱신합니다.");
                String url = buildUrl(MARKET_ALL_ENDPOINT);
                UpbitMarket[] markets = restTemplate.getForObject(url, UpbitMarket[].class);

                if (markets != null) {
                    // 새 캐시 맵 생성
                    Map<String, String> newCache = Arrays.stream(markets)
                            .filter(m -> m.getMarket().startsWith("KRW-"))
                            .collect(Collectors.toMap(
                                m -> m.getMarket().split("-")[1],
                                UpbitMarket::getKoreanName,
                                (existing, replacement) -> existing
                            ));

                    // 캐시 업데이트
                    coinNameCache.clear();
                    coinNameCache.putAll(newCache);
                    lastCacheRefresh = LocalDateTime.now();
                    log.info("업비트 코인 이름 정보 캐시 갱신 완료: {} 개 코인", coinNameCache.size());
                }
            } catch (Exception e) {
                log.error("업비트 코인 이름 정보를 가져오는 중 오류가 발생했습니다", e);
                // 캐시가 비어있는 경우 빈 맵 반환
                if (coinNameCache.isEmpty()) {
                    return new HashMap<>();
                }
                // 이전 캐시 데이터 계속 사용
            }
        }
        
        return coinNameCache;
    }
    
    /**
     * 주요 코인들의 현재 시세 정보를 조회합니다.
     */
    public List<ExchangePriceDto> getTopCoinPrices() {
        // 주요 코인 목록
        List<String> topCoins = List.of("KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-SOL", 
                "KRW-ADA", "KRW-DOGE", "KRW-DOT", "KRW-MATIC", "KRW-AVAX", "KRW-TRX");
        
        return getTickers(topCoins);
    }
}
