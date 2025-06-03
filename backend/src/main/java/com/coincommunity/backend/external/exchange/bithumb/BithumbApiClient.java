package com.coincommunity.backend.external.exchange.bithumb;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.dto.ExchangePriceDto.TradingStatus;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 빗썸 API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://apidocs.bithumb.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BithumbApiClient implements ExchangeApiStrategy {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.bithumb.base-url}")
    private String baseUrl;
    
    @Value("${external.bithumb.version}")
    private String version;
    
    private static final String EXCHANGE = "BITHUMB";
    
    // API 엔드포인트 상수 정의
    private static final String TICKER_ALL_ENDPOINT = "/ticker/ALL_KRW";
    private static final String TICKER_ENDPOINT = "/ticker";
    
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
        try {
            String url = buildUrl(TICKER_ALL_ENDPOINT);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.path("data");
                
                List<String> coins = new ArrayList<>();
                data.fieldNames().forEachRemaining(fieldName -> {
                    if (!"date".equals(fieldName)) {
                        coins.add(fieldName);
                    }
                });
                return coins;
            }
        } catch (Exception e) {
            log.error("빗썸 지원 코인 목록 조회 실패", e);
        }
        
        // 기본 코인 목록 반환
        return List.of("BTC", "ETH", "XRP", "ADA", "DOT", "SOL", "DOGE", "MATIC", "AVAX", "TRX");
    }
    
    @Override
    public List<ExchangePriceDto> getAllCoinPrices() {
        try {
            String url = buildUrl(TICKER_ALL_ENDPOINT);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.path("data");
                
                List<ExchangePriceDto> prices = new ArrayList<>();
                data.fieldNames().forEachRemaining(symbol -> {
                    if (!"date".equals(symbol)) {
                        JsonNode coinData = data.path(symbol);
                        ExchangePriceDto exchangePrice = parseExchangePrice(symbol, coinData);
                        if (exchangePrice != null) {
                            prices.add(exchangePrice);
                        }
                    }
                });
                
                return prices;
            }
        } catch (Exception e) {
            log.error("빗썸 전체 코인 가격 조회 실패", e);
        }
        
        return new ArrayList<>();
    }
    
    @Override
    public ExchangePriceDto getCoinPrice(String coinSymbol) {
        try {
            String url = buildUrl(TICKER_ENDPOINT + "/" + coinSymbol + "_KRW");
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.path("data");
                
                return parseExchangePrice(coinSymbol, data);
            }
        } catch (Exception e) {
            log.error("빗썸 코인 가격 조회 실패: {}", coinSymbol, e);
        }
        
        return null;
    }
    
    @Override
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        List<ExchangePriceDto> allPrices = getAllCoinPrices();
        return allPrices.stream()
                .sorted((a, b) -> {
                    // 거래량 기준으로 정렬 (시가총액 데이터가 없으므로)
                    BigDecimal volumeA = a.getVolume24h() != null ? a.getVolume24h() : BigDecimal.ZERO;
                    BigDecimal volumeB = b.getVolume24h() != null ? b.getVolume24h() : BigDecimal.ZERO;
                    return volumeB.compareTo(volumeA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isHealthy() {
        try {
            String url = buildUrl(TICKER_ENDPOINT + "/BTC_KRW");
            String response = restTemplate.getForObject(url, String.class);
            return response != null && response.contains("status") && response.contains("0000");
        } catch (Exception e) {
            log.warn("빗썸 API 건강 상태 확인 실패", e);
            return false;
        }
    }

    /**
     * 빗썸 API 응답에서 ExchangePriceDto 객체 생성
     */
    private ExchangePriceDto parseExchangePrice(String symbol, JsonNode data) {
        try {
            // 현재가
            String closingPriceStr = data.path("closing_price").asText();
            BigDecimal currentPrice = !closingPriceStr.isEmpty() ? new BigDecimal(closingPriceStr) : BigDecimal.ZERO;
            
            // 변화율
            String fluctateRateStr = data.path("fluctate_rate_24H").asText();
            BigDecimal changeRate = !fluctateRateStr.isEmpty() ? new BigDecimal(fluctateRateStr) : BigDecimal.ZERO;
            
            // 거래량
            String unitsTraded24HStr = data.path("units_traded_24H").asText();
            BigDecimal volume24h = !unitsTraded24HStr.isEmpty() ? new BigDecimal(unitsTraded24HStr) : BigDecimal.ZERO;
            
            // 최고가
            String maxPriceStr = data.path("max_price").asText();
            BigDecimal highPrice24h = !maxPriceStr.isEmpty() ? new BigDecimal(maxPriceStr) : BigDecimal.ZERO;
            
            // 최저가
            String minPriceStr = data.path("min_price").asText();
            BigDecimal lowPrice24h = !minPriceStr.isEmpty() ? new BigDecimal(minPriceStr) : BigDecimal.ZERO;
            
            return ExchangePriceDto.builder()
                    .exchangeName(EXCHANGE)
                    .exchangeKoreanName("빗썸")
                    .exchangeType(com.coincommunity.backend.dto.ExchangePriceDto.ExchangeType.DOMESTIC)
                    .symbol(symbol)
                    .koreanName(getKoreanName(symbol))
                    .currentPrice(currentPrice)
                    .changeRate(changeRate)
                    .highPrice24h(highPrice24h)
                    .lowPrice24h(lowPrice24h)
                    .volume24h(volume24h)
                    .status(TradingStatus.NORMAL)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("빗썸 코인 데이터 파싱 실패: {}", symbol, e);
            return null;
        }
    }

    /**
     * 코인 심볼에 대한 한글명 반환
     */
    private String getKoreanName(String symbol) {
        Map<String, String> koreanNames = Map.of(
            "BTC", "비트코인",
            "ETH", "이더리움",
            "XRP", "리플",
            "ADA", "에이다",
            "DOT", "폴카닷",
            "SOL", "솔라나",
            "DOGE", "도지코인",
            "MATIC", "폴리곤",
            "AVAX", "아발란체",
            "TRX", "트론"
        );
        return koreanNames.getOrDefault(symbol, symbol);
    }
}
