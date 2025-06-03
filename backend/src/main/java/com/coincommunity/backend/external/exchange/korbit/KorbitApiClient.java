package com.coincommunity.backend.external.exchange.korbit;

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
 * 코빗 API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://apidocs.korbit.co.kr/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KorbitApiClient implements ExchangeApiStrategy {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.korbit.base-url}")
    private String baseUrl;
    
    @Value("${external.korbit.version}")
    private String version;
    
    private static final String EXCHANGE = "KORBIT";
    
    // API 엔드포인트 상수 정의
    private static final String TICKER_DETAILED_ALL_ENDPOINT = "/ticker/detailed/all";
    private static final String TICKER_DETAILED_ENDPOINT = "/ticker";
    
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
            String url = buildUrl(TICKER_DETAILED_ALL_ENDPOINT);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                List<String> coins = new ArrayList<>();
                
                root.fieldNames().forEachRemaining(fieldName -> {
                    // "btc_krw" 형태에서 "BTC" 추출
                    if (fieldName.endsWith("_krw")) {
                        String coin = fieldName.replace("_krw", "").toUpperCase();
                        coins.add(coin);
                    }
                });
                
                return coins;
            }
        } catch (Exception e) {
            log.error("코빗 지원 코인 목록 조회 실패", e);
        }
        
        // 기본 코인 목록 반환
        return List.of("BTC", "ETH", "XRP", "ADA", "DOT", "SOL", "DOGE", "MATIC", "AVAX", "TRX");
    }

    @Override
    public List<ExchangePriceDto> getAllCoinPrices() {
        try {
            String url = buildUrl(TICKER_DETAILED_ALL_ENDPOINT);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                List<ExchangePriceDto> prices = new ArrayList<>();
                
                root.fieldNames().forEachRemaining(fieldName -> {
                    if (fieldName.endsWith("_krw")) {
                        String symbol = fieldName.replace("_krw", "").toUpperCase();
                        JsonNode coinData = root.path(fieldName);
                        ExchangePriceDto exchangePrice = parseExchangePrice(symbol, coinData);
                        if (exchangePrice != null) {
                            prices.add(exchangePrice);
                        }
                    }
                });
                
                return prices;
            }
        } catch (Exception e) {
            log.error("코빗 전체 코인 가격 조회 실패", e);
        }
        
        return new ArrayList<>();
    }

    @Override
    public ExchangePriceDto getCoinPrice(String coinSymbol) {
        try {
            // 개별 코인 가격 조회를 위한 URL 수정
            String url = buildUrl(TICKER_DETAILED_ENDPOINT + "?currency_pair=" + coinSymbol.toLowerCase() + "_krw");
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                return parseExchangePrice(coinSymbol, root);
            }
        } catch (Exception e) {
            log.error("코빗 코인 가격 조회 실패: {}", coinSymbol, e);
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
            // 코빗 API v1의 정확한 엔드포인트 사용
            String url = buildUrl(TICKER_DETAILED_ENDPOINT);
            log.debug("코빗 헬스체크 URL: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            boolean isHealthy = response != null && !response.contains("Oops, the page you requested is missing");
            log.debug("코빗 헬스체크 결과: {}, 응답: {}", isHealthy, response != null ? response.substring(0, Math.min(100, response.length())) : "null");
            return isHealthy;
        } catch (Exception e) {
            log.warn("코빗 API 건강 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 코빗 API 응답에서 ExchangePriceDto 객체 생성
     */
    private ExchangePriceDto parseExchangePrice(String symbol, JsonNode data) {
        try {
            // 현재가 (last)
            String lastStr = data.path("last").asText();
            BigDecimal currentPrice = !lastStr.isEmpty() ? new BigDecimal(lastStr) : BigDecimal.ZERO;
            
            // 변화율 (change)
            String changeStr = data.path("change").asText();
            BigDecimal changeRate = !changeStr.isEmpty() ? new BigDecimal(changeStr) : BigDecimal.ZERO;
            
            // 거래량 (volume)
            String volumeStr = data.path("volume").asText();
            BigDecimal volume24h = !volumeStr.isEmpty() ? new BigDecimal(volumeStr) : BigDecimal.ZERO;
            
            // 최고가 (high)
            String highStr = data.path("high").asText();
            BigDecimal highPrice24h = !highStr.isEmpty() ? new BigDecimal(highStr) : BigDecimal.ZERO;
            
            // 최저가 (low)
            String lowStr = data.path("low").asText();
            BigDecimal lowPrice24h = !lowStr.isEmpty() ? new BigDecimal(lowStr) : BigDecimal.ZERO;
            
            return ExchangePriceDto.builder()
                    .exchangeName(EXCHANGE)
                    .exchangeKoreanName("코빗")
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
            log.error("코빗 코인 데이터 파싱 실패: {}", symbol, e);
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
