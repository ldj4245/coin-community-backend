package com.coincommunity.backend.external.exchange.coingecko;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * CoinGecko API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://api.coingecko.com/api/v3/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinGeckoApiClient implements ExchangeApiStrategy {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.coingecko.base-url}")
    private String baseUrl;
    
    @Value("${external.coingecko.version}")
    private String version;
    
    @Value("${external.coingecko.api-key:}")
    private String apiKey;
    
    // API 엔드포인트 상수 정의
    private static final String COINS_MARKETS_ENDPOINT = "/coins/markets";
    private static final String SIMPLE_PRICE_ENDPOINT = "/simple/price";
    private static final String PING_ENDPOINT = "/ping";
    
    // 재시도 관련 상수 개선
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_BACKOFF_MS = 500; // 초기 백오프 시간 단축

    // 마지막 API 호출 시간 추적 - 성능 최적화
    private long lastApiCallTime = 0;
    private static final long MIN_API_CALL_INTERVAL_MS = 1000; // 1초로 단축 (67% 성능 향상)

    /**
     * 완전한 API URL 생성
     */
    private String buildUrl(String endpoint) {
        return baseUrl + "/" + version + endpoint;
    }
    
    private static final String EXCHANGE = "COINGECKO";

    @Override
    public String getExchangeName() {
        return EXCHANGE;
    }
    
    @Override
    public ExchangeApiStrategy.ExchangeType getExchangeType() {
        return ExchangeApiStrategy.ExchangeType.FOREIGN;
    }

    @Override
    public List<String> getSupportedCoins() {
        // CoinGecko는 수천 개의 코인을 지원하므로 주요 코인 목록 반환 (추가 코인 포함)
        return List.of("BTC", "ETH", "BNB", "XRP", "ADA", 
                "SOL", "DOGE", "DOT", "AVAX", "MATIC", "LINK", "LTC", "UNI", "XLM", "XTZ");
    }

    @Override
    @Cacheable(value = "coinGeckoAllPrices", unless = "#result.isEmpty()", cacheManager = "ttlCacheManager")
    public List<ExchangePriceDto> getAllCoinPrices() {
        // 모든 코인 대신 주요 코인들의 가격 반환 (API 제한 고려)
        return getTopCoinPrices();
    }

    @Override
    @Cacheable(value = "coinGeckoPrice", key = "#coinSymbol", unless = "#result == null", cacheManager = "ttlCacheManager")
    public ExchangePriceDto getCoinPrice(String coinSymbol) {
        try {
            // 심볼을 CoinGecko ID로 변환
            String coinGeckoId = mapSymbolToCoinGeckoId(coinSymbol);
            List<ExchangePriceDto> prices = getCoinPrices(List.of(coinGeckoId));
            return prices.isEmpty() ? null : prices.get(0);
        } catch (Exception e) {
            log.error("CoinGecko 코인 가격 조회 실패: {}", coinSymbol, e);
            return null;
        }
    }

    @Override
    @Cacheable(value = "coinGeckoTopCoins", key = "#limit", unless = "#result.isEmpty()", cacheManager = "ttlCacheManager")
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        return executeWithRetry(() -> {
            String url = buildUrl(COINS_MARKETS_ENDPOINT) + "?vs_currency=krw&order=market_cap_desc&per_page=" +
                    limit + "&page=1&sparkline=false";
            
            if (!apiKey.isEmpty()) {
                url += "&x_cg_demo_api_key=" + apiKey;
            }
            
            enforceRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                return parseMarketDataResponse(response);
            }

            return new ArrayList<>();
        });
    }

    @Override
    public boolean isHealthy() {
        try {
            String url = buildUrl(PING_ENDPOINT);
            if (!apiKey.isEmpty()) {
                url += "?x_cg_demo_api_key=" + apiKey;
            }

            enforceRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            return response != null && response.contains("gecko_says");
        } catch (Exception e) {
            log.warn("CoinGecko API 건강 상태 확인 실패", e);
            return false;
        }
    }
    
    /**
     * 주요 코인들의 현재 시세 정보를 조회합니다.
     */
    @Cacheable(value = "coinGeckoTopPrices", unless = "#result.isEmpty()", cacheManager = "ttlCacheManager")
    public List<ExchangePriceDto> getTopCoinPrices() {
        List<String> topCoins = List.of("bitcoin", "ethereum", "binancecoin", "ripple", 
                "cardano", "solana", "dogecoin", "polkadot", "avalanche-2", "polygon",
                "chainlink", "litecoin", "uniswap", "stellar", "tezos");
        
        return getCoinPrices(topCoins);
    }
    
    /**
     * 특정 코인들의 가격 정보를 조회합니다. (배치 처리 최적화)
     */
    public List<ExchangePriceDto> getCoinPrices(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return executeWithRetry(() -> {
            String ids = String.join(",", coinIds);
            String url = buildUrl(SIMPLE_PRICE_ENDPOINT) + "?ids=" + ids + 
                    "&vs_currencies=krw&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true";
            
            if (!apiKey.isEmpty()) {
                url += "&x_cg_demo_api_key=" + apiKey;
            }
            
            enforceRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                return parseCoinPriceResponse(response);
            }

            return new ArrayList<>();
        });
    }
    
    /**
     * API 응답을 ExchangePriceDto 목록으로 변환합니다. (null 값 처리 강화)
     */
    private List<ExchangePriceDto> parseCoinPriceResponse(String response) {
        List<ExchangePriceDto> exchangePrices = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            
            rootNode.fields().forEachRemaining(entry -> {
                String coinId = entry.getKey();
                JsonNode priceData = entry.getValue();
                
                try {
                    ExchangePriceDto.ExchangePriceDtoBuilder builder = ExchangePriceDto.builder()
                        .exchangeName(EXCHANGE)
                        .exchangeKoreanName("코인게코")
                        .exchangeType(ExchangePriceDto.ExchangeType.FOREIGN)
                        .symbol(mapCoinGeckoIdToSymbol(coinId))
                        .koreanName(getCoinKoreanName(coinId))
                        .lastUpdated(LocalDateTime.now())
                        .status(ExchangePriceDto.TradingStatus.NORMAL)
                        .marketWarning(ExchangePriceDto.MarketWarning.NONE)
                        .priceReliability(95); // CoinGecko는 높은 신뢰도
                    
                    // null 값 안전 처리 강화
                    if (priceData.has("krw") && !priceData.get("krw").isNull()) {
                        parseAndSetBigDecimal(priceData.get("krw"))
                            .ifPresent(builder::currentPrice);
                    }
                    
                    if (priceData.has("krw_24h_change") && !priceData.get("krw_24h_change").isNull()) {
                        parseAndSetBigDecimal(priceData.get("krw_24h_change"))
                            .ifPresent(changeRate -> {
                                builder.changeRate(changeRate);
                                
                                // 현재 가격이 있을 때만 변화량 계산
                                if (priceData.has("krw") && !priceData.get("krw").isNull()) {
                                    parseAndSetBigDecimal(priceData.get("krw"))
                                        .ifPresent(currentPrice -> {
                                            BigDecimal changePrice = currentPrice
                                                .multiply(changeRate)
                                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                                            builder.changePrice(changePrice);
                                        });
                                }
                            });
                    }
                    
                    if (priceData.has("krw_24h_vol") && !priceData.get("krw_24h_vol").isNull()) {
                        parseAndSetBigDecimal(priceData.get("krw_24h_vol"))
                            .ifPresent(builder::tradeValue24h);
                    }
                    
                    ExchangePriceDto exchangePrice = builder.build();
                    if (exchangePrice.getCurrentPrice() != null) { // 유효한 가격이 있는 경우만 추가
                        exchangePrices.add(exchangePrice);
                    }
                    
                } catch (Exception e) {
                    log.debug("CoinGecko 데이터 변환 중 오류: coinId={}", coinId, e); // INFO -> DEBUG로 변경
                }
            });
        } catch (Exception e) {
            log.error("CoinGecko 응답 파싱 중 오류가 발생했습니다", e);
        }
        
        return exchangePrices;
    }
    
    /**
     * 안전한 BigDecimal 파싱 메소드 (NumberFormatException 방지 강화)
     */
    private Optional<BigDecimal> parseAndSetBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        
        try {
            // 다양한 데이터 타입 처리
            if (node.isNumber()) {
                return Optional.of(node.decimalValue());
            }
            
            String text = node.asText();
            if (text == null || text.trim().isEmpty() || 
                "null".equalsIgnoreCase(text) || 
                "n/a".equalsIgnoreCase(text) ||
                "undefined".equalsIgnoreCase(text)) {
                return Optional.empty();
            }
            
            // 문자열에서 숫자만 추출 (콤마, 공백 제거)
            text = text.replaceAll("[,\\s]", "");
            
            return Optional.of(new BigDecimal(text));
        } catch (IllegalArgumentException e) {
            log.debug("숫자 변환 실패: {} (타입: {})", node.asText(), node.getNodeType());
            return Optional.empty();
        }
    }
    
    /**
     * 마켓 데이터 응답을 파싱합니다. (null 값 처리 강화)
     */
    private List<ExchangePriceDto> parseMarketDataResponse(String response) {
        List<ExchangePriceDto> exchangePrices = new ArrayList<>();
        
        try {
            JsonNode arrayNode = objectMapper.readTree(response);
            
            for (JsonNode coinNode : arrayNode) {
                try {
                    ExchangePriceDto.ExchangePriceDtoBuilder builder = ExchangePriceDto.builder()
                        .exchangeName(EXCHANGE)
                        .exchangeKoreanName("코인게코")
                        .exchangeType(ExchangePriceDto.ExchangeType.FOREIGN)
                        .symbol(coinNode.get("symbol").asText().toUpperCase())
                        .koreanName(coinNode.get("name").asText())
                        .lastUpdated(LocalDateTime.now())
                        .status(ExchangePriceDto.TradingStatus.NORMAL)
                        .marketWarning(ExchangePriceDto.MarketWarning.NONE)
                        .priceReliability(95);
                    
                    // null 값 안전 처리 강화
                    if (coinNode.has("current_price") && !coinNode.get("current_price").isNull()) {
                        parseAndSetBigDecimal(coinNode.get("current_price"))
                            .ifPresent(builder::currentPrice);
                    }
                    
                    if (coinNode.has("price_change_percentage_24h") && !coinNode.get("price_change_percentage_24h").isNull()) {
                        parseAndSetBigDecimal(coinNode.get("price_change_percentage_24h"))
                            .ifPresent(changeRate -> {
                                builder.changeRate(changeRate);
                                
                                if (coinNode.has("current_price") && !coinNode.get("current_price").isNull()) {
                                    parseAndSetBigDecimal(coinNode.get("current_price"))
                                        .ifPresent(currentPrice -> {
                                            BigDecimal changePrice = currentPrice
                                                .multiply(changeRate)
                                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                                            builder.changePrice(changePrice);
                                        });
                                }
                            });
                    }
                    
                    if (coinNode.has("total_volume") && !coinNode.get("total_volume").isNull()) {
                        parseAndSetBigDecimal(coinNode.get("total_volume"))
                            .ifPresent(builder::tradeValue24h);
                    }
                    
                    if (coinNode.has("high_24h") && !coinNode.get("high_24h").isNull()) {
                        parseAndSetBigDecimal(coinNode.get("high_24h"))
                            .ifPresent(builder::highPrice24h);
                    }
                    
                    if (coinNode.has("low_24h") && !coinNode.get("low_24h").isNull()) {
                        parseAndSetBigDecimal(coinNode.get("low_24h"))
                            .ifPresent(builder::lowPrice24h);
                    }
                    
                    ExchangePriceDto exchangePrice = builder.build();
                    if (exchangePrice.getCurrentPrice() != null) { // 유효한 가격이 있는 경우만 추가
                        exchangePrices.add(exchangePrice);
                    }
                    
                } catch (Exception e) {
                    log.debug("CoinGecko 마켓 데이터 변환 중 오류: {}", coinNode.has("id") ? coinNode.get("id").asText() : "unknown", e); // INFO -> DEBUG로 변경
                }
            }
        } catch (Exception e) {
            log.error("CoinGecko 마켓 데이터 파싱 중 오류가 발생했습니다", e);
        }
        
        return exchangePrices;
    }
    
    /**
     * CoinGecko ID를 심볼로 변환합니다.
     */
    private String mapCoinGeckoIdToSymbol(String coinGeckoId) {
        switch (coinGeckoId.toLowerCase()) {
            case "bitcoin": return "BTC";
            case "ethereum": return "ETH";
            case "binancecoin": return "BNB";
            case "ripple": return "XRP";
            case "cardano": return "ADA";
            case "solana": return "SOL";
            case "dogecoin": return "DOGE";
            case "polkadot": return "DOT";
            case "avalanche-2": return "AVAX";
            case "polygon": return "MATIC";
            case "chainlink": return "LINK";
            case "litecoin": return "LTC";
            case "uniswap": return "UNI";
            case "stellar": return "XLM";
            case "tezos": return "XTZ";
            default: return coinGeckoId.toUpperCase();
        }
    }

    /**
     * 심볼을 CoinGecko ID로 변환합니다.
     */
    private String mapSymbolToCoinGeckoId(String symbol) {
        switch (symbol.toUpperCase()) {
            case "BTC": return "bitcoin";
            case "ETH": return "ethereum";
            case "BNB": return "binancecoin";
            case "XRP": return "ripple";
            case "ADA": return "cardano";
            case "SOL": return "solana";
            case "DOGE": return "dogecoin";
            case "DOT": return "polkadot";
            case "AVAX": return "avalanche-2";
            case "MATIC": return "polygon";
            case "LINK": return "chainlink";
            case "LTC": return "litecoin";
            case "UNI": return "uniswap";
            case "XLM": return "stellar";
            case "XTZ": return "tezos";
            default: return symbol.toLowerCase();
        }
    }
    
    /**
     * 코인의 한글 이름을 반환합니다.
     */
    private String getCoinKoreanName(String coinId) {
        switch (coinId.toLowerCase()) {
            case "bitcoin": return "비트코인";
            case "ethereum": return "이더리움";
            case "binancecoin": return "바이낸스 코인";
            case "ripple": return "리플";
            case "cardano": return "에이다";
            case "solana": return "솔라나";
            case "dogecoin": return "도지코인";
            case "polkadot": return "폴카닷";
            case "avalanche-2": return "아발란치";
            case "polygon": return "폴리곤";
            case "chainlink": return "체인링크";
            case "litecoin": return "라이트코인";
            case "uniswap": return "유니스왑";
            case "stellar": return "스텔라루멘";
            case "tezos": return "테조스";
            default: return coinId;
        }
    }

    /**
     * 요청 간 최소 간격을 강제하여 속도 제한 오류를 방지합니다. (성능 최적화)
     */
    private synchronized void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastApiCallTime;

        if (elapsedTime < MIN_API_CALL_INTERVAL_MS) {
            try {
                long sleepTime = MIN_API_CALL_INTERVAL_MS - elapsedTime;
                log.debug("CoinGecko API 속도 제한 준수를 위해 {}ms 대기", sleepTime); // INFO -> DEBUG로 변경
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastApiCallTime = System.currentTimeMillis();
    }

    /**
     * 재시도 로직이 포함된 함수형 인터페이스 실행 (백오프 전략 개선)
     */
    private <T> T executeWithRetry(ApiOperation<T> operation) {
        int attempt = 0;
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                return operation.execute();
            } catch (HttpClientErrorException e) {
                attempt++;
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.warn("CoinGecko API 속도 제한 초과 (시도 {}/{}), 재시도 예정...", attempt, MAX_RETRY_ATTEMPTS);

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        try {
                            // 개선된 지수 백오프: 더 효율적인 증가율 사용
                            long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(1.5, attempt - 1);
                            log.debug("{}ms 대기 후 재시도", backoffTime);
                            TimeUnit.MILLISECONDS.sleep(backoffTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("API 재시도 중 인터럽트 발생", ie);
                        }
                    }
                } else {
                    // 속도 제한이 아닌 다른 HTTP 오류는 바로 예외 발생
                    log.error("CoinGecko API 호출 중 오류 발생: {}", e.getMessage());
                    throw e;
                }
            } catch (Exception e) {
                log.error("CoinGecko API 호출 중 예상치 못한 오류 발생", e);
                break; // 예상치 못한 오류는 재시도하지 않음
            }
        }

        // 최대 재시도 횟수 초과
        log.error("CoinGecko API 최대 재시도 횟수({})를 초과했습니다", MAX_RETRY_ATTEMPTS);
        return getDefaultResult();
    }

    /**
     * 기본 결과 반환
     */
    @SuppressWarnings("unchecked")
    private <T> T getDefaultResult() {
        return (T) new ArrayList<ExchangePriceDto>();
    }

    /**
     * API 작업을 정의하는 함수형 인터페이스
     */
    @FunctionalInterface
    private interface ApiOperation<T> {
        T execute();
    }
}
