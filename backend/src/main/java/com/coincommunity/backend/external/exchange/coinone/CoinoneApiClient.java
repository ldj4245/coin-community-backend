package com.coincommunity.backend.external.exchange.coinone;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 코인원 거래소 API 클라이언트
 * 국내 거래소로 KRW 기준 가격 정보를 제공합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoinoneApiClient implements ExchangeApiStrategy {

    private final RestTemplate restTemplate;

    @Value("${external.coinone.base-url}")
    private String baseUrl;
    
    @Value("${external.coinone.version}")
    private String version;

    // API 엔드포인트 상수 정의
    private static final String TICKER_ENDPOINT = "/ticker";
    private static final String ORDERBOOK_ENDPOINT = "/orderbook";
    
    /**
     * 완전한 API URL 생성
     */
    private String buildUrl(String endpoint) {
        return baseUrl + "/" + version + endpoint;
    }

    // 코인원에서 지원하는 주요 코인 목록
    private static final List<String> SUPPORTED_COINS = List.of(
        "BTC", "ETH", "ETC", "XRP", "BCH", "LTC", "QTUM", "IOTA", "BTG", "EOS",
        "TRX", "ELF", "KNC", "GLM", "ZIL", "WAXP", "POWR", "LRC", "GTO", "STEEM"
    );

    // 코인별 한글명 매핑
    private static final Map<String, String> COIN_KOREAN_NAMES = Map.of(
        "BTC", "비트코인",
        "ETH", "이더리움",
        "ETC", "이더리움클래식",
        "XRP", "리플",
        "BCH", "비트코인캐시",
        "LTC", "라이트코인",
        "QTUM", "퀀텀",
        "IOTA", "아이오타",
        "BTG", "비트코인골드",
        "EOS", "이오스"
    );

    @Override
    public String getExchangeName() {
        return "COINONE";
    }

    @Override
    public ExchangeApiStrategy.ExchangeType getExchangeType() {
        return ExchangeApiStrategy.ExchangeType.DOMESTIC;
    }

    @Override
    public List<String> getSupportedCoins() {
        return new ArrayList<>(SUPPORTED_COINS);
    }

    @Override
    public List<ExchangePriceDto> getAllCoinPrices() {
        log.info("코인원 전체 코인 가격 조회 요청");

        List<ExchangePriceDto> prices = new ArrayList<>();

        try {
            String url = buildUrl(TICKER_ENDPOINT + "?currency=all");
            log.debug("코인원 전체 가격 정보 API 호출: {}", url);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.debug("코인원 API 응답 데이터: {}", responseBody);

                if ("success".equals(responseBody.get("result")) || "0".equals(responseBody.get("errorCode"))) {
                    // data 배열에서 코인 정보 추출
                    if (responseBody.containsKey("data") && responseBody.get("data") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");

                        for (Map<String, Object> coinData : dataList) {
                            if (coinData.containsKey("market")) {
                                String market = (String) coinData.get("market");
                                // "KRW-BTC" 형태에서 "BTC" 추출
                                String[] marketParts = market.split("-");
                                if (marketParts.length > 1) {
                                    String symbol = marketParts[1];
                                    log.debug("코인 심볼 확인: {}", symbol);

                                    ExchangePriceDto priceDto = createPriceDtoFromNewFormat(coinData, symbol);
                                    if (priceDto != null) {
                                        prices.add(priceDto);
                                        log.debug("코인 추가됨: {}", symbol);
                                    }
                                }
                            }
                        }
                    } else {
                        log.warn("코인원 API 응답에 'data' 배열이 없거나 형식이 다릅니다: {}", responseBody);
                    }
                }

                log.info("코인원 가격 정보 조회 완료 - 코인 수: {}", prices.size());
                if (prices.isEmpty()) {
                    log.warn("코인원 가격 정보가 없습니다. API 응답 구조 확인 필요: {}", responseBody);
                }
            }

        } catch (Exception e) {
            log.error("코인원 전체 가격 정보 조회 실패", e);
        }

        return prices;
    }

    @Override
    public ExchangePriceDto getCoinPrice(String symbol) {
        log.info("코인원 개별 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        String normalizedSymbol = symbol.toUpperCase().replace("KRW-", "");

        try {
            String url = buildUrl(TICKER_ENDPOINT + "?currency=" + normalizedSymbol.toLowerCase());
            log.debug("코인원 API 호출: {}", url);
            
            long startTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.getForEntity(url, Map.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                
                if ("success".equals(data.get("result"))) {
                    ExchangePriceDto priceDto = createPriceDto(data, normalizedSymbol);
                    if (priceDto != null) {
                        // 응답 시간 설정
                        priceDto = ExchangePriceDto.builder()
                            .exchangeName(priceDto.getExchangeName())
                            .exchangeKoreanName(priceDto.getExchangeKoreanName())
                            .exchangeType(priceDto.getExchangeType())
                            .symbol(priceDto.getSymbol())
                            .koreanName(priceDto.getKoreanName())
                            .currentPrice(priceDto.getCurrentPrice())
                            .changePrice(priceDto.getChangePrice())
                            .changeRate(priceDto.getChangeRate())
                            .highPrice24h(priceDto.getHighPrice24h())
                            .lowPrice24h(priceDto.getLowPrice24h())
                            .volume24h(priceDto.getVolume24h())
                            .tradeValue24h(priceDto.getTradeValue24h())
                            .bidPrice(priceDto.getBidPrice())
                            .askPrice(priceDto.getAskPrice())
                            .spread(priceDto.getSpread())
                            .spreadRate(priceDto.getSpreadRate())
                            .status(priceDto.getStatus())
                            .marketWarning(priceDto.getMarketWarning())
                            .lastUpdated(priceDto.getLastUpdated())
                            .priceReliability(priceDto.getPriceReliability())
                            .responseTime(responseTime)
                            .build();
                    }
                    return priceDto;
                }
            }
            
        } catch (Exception e) {
            log.error("코인원 개별 코인 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }
        
        return null;
    }

    @Override
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        log.info("코인원 거래량 상위 코인 조회 요청 - 개수: {}", limit);
        
        // 코인원 API는 시가총액 순으로 정렬된 데이터를 제공하지 않으므로
        // 전체 데이터를 가져와서 거래액 기준으로 정렬
        List<ExchangePriceDto> allPrices = getAllCoinPrices();
        
        return allPrices.stream()
            .sorted((a, b) -> b.getTradeValue24h().compareTo(a.getTradeValue24h()))
            .limit(limit)
            .toList();
    }

    @Override
    public boolean isHealthy() {
        try {
            String url = buildUrl(TICKER_ENDPOINT + "?currency=btc");
            log.debug("코인원 헬스체크 API 호출: {}", url);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                boolean isHealthy = "success".equals(data.get("result"));
                
                log.info("코인원 API 상태 확인 - 정상: {}", isHealthy);
                return isHealthy;
            }
            
        } catch (Exception e) {
            log.error("코인원 API 헬스체크 실패", e);
        }
        
        return false;
    }

    /**
     * 코인원 API 응답 데이터를 ExchangePriceDto로 변환
     */
    private ExchangePriceDto createPriceDto(Map<String, Object> data, String symbol) {
        try {
            BigDecimal last = new BigDecimal(data.get("last").toString());
            BigDecimal yesterdayLast = new BigDecimal(data.get("yesterday_last").toString());
            BigDecimal high = new BigDecimal(data.get("high").toString());
            BigDecimal low = new BigDecimal(data.get("low").toString());
            BigDecimal volume = new BigDecimal(data.get("volume").toString());

            // 변화율 계산
            BigDecimal changePrice = last.subtract(yesterdayLast);
            BigDecimal changeRate = (yesterdayLast.compareTo(BigDecimal.ZERO) > 0) ?
                    changePrice.divide(yesterdayLast, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                    BigDecimal.ZERO;

            return ExchangePriceDto.builder()
                    .exchangeName("COINONE")
                    .exchangeKoreanName("코인원")
                    .exchangeType(ExchangePriceDto.ExchangeType.DOMESTIC)
                    .symbol(symbol.toUpperCase())
                    .koreanName(COIN_KOREAN_NAMES.getOrDefault(symbol.toUpperCase(), symbol.toUpperCase()))
                    .currentPrice(last)
                    .changePrice(changePrice)
                    .changeRate(changeRate)
                    .highPrice24h(high)
                    .lowPrice24h(low)
                    .volume24h(volume)
                    .tradeValue24h(volume.multiply(last))
                    // 실제 응답에 없으므로 기본값 처리
                    .bidPrice(BigDecimal.ZERO)
                    .askPrice(BigDecimal.ZERO)
                    .spread(BigDecimal.ZERO)
                    .spreadRate(BigDecimal.ZERO)
                    .status(ExchangePriceDto.TradingStatus.NORMAL)
                    .marketWarning(ExchangePriceDto.MarketWarning.NONE)
                    .lastUpdated(LocalDateTime.now())
                    .priceReliability(92) // 중간 정도 신뢰도 유지
                    .build();

        } catch (Exception e) {
            log.error("코인원 가격 데이터 변환 실패 - 심볼: {}", symbol, e);
            return null;
        }
    }

    /**
     * 새로운 API 응답 형식에서 ExchangePriceDto 생성
     */
    private ExchangePriceDto createPriceDtoFromNewFormat(Map<String, Object> data, String symbol) {
        try {
            BigDecimal open = new BigDecimal(data.get("open_24h").toString());
            BigDecimal close = new BigDecimal(data.get("close_24h").toString());
            BigDecimal high = new BigDecimal(data.get("high_24h").toString());
            BigDecimal low = new BigDecimal(data.get("low_24h").toString());
            BigDecimal volume = new BigDecimal(data.get("volume_24h").toString());
            BigDecimal baseVolume = new BigDecimal(data.get("base_volume_24h").toString());
            BigDecimal change = new BigDecimal(data.get("change_24h").toString());
            BigDecimal changeRate = new BigDecimal(data.get("change_rate_24h").toString());

            return ExchangePriceDto.builder()
                    .exchangeName("COINONE")
                    .exchangeKoreanName("코인원")
                    .exchangeType(ExchangePriceDto.ExchangeType.DOMESTIC)
                    .symbol(symbol.toUpperCase())
                    .koreanName(COIN_KOREAN_NAMES.getOrDefault(symbol.toUpperCase(), symbol.toUpperCase()))
                    .currentPrice(close)
                    .changePrice(change)
                    .changeRate(changeRate)
                    .highPrice24h(high)
                    .lowPrice24h(low)
                    .volume24h(volume)
                    .tradeValue24h(baseVolume)
                    // 실제 응답에 없으므로 기본값 처리
                    .bidPrice(BigDecimal.ZERO)
                    .askPrice(BigDecimal.ZERO)
                    .spread(BigDecimal.ZERO)
                    .spreadRate(BigDecimal.ZERO)
                    .status(ExchangePriceDto.TradingStatus.NORMAL)
                    .marketWarning(ExchangePriceDto.MarketWarning.NONE)
                    .lastUpdated(LocalDateTime.now())
                    .priceReliability(92) // 중간 정도 신뢰도 유지
                    .build();

        } catch (Exception e) {
            log.error("코인원 새 형식 가격 데이터 변환 실패 - 심볼: {}, 데이터: {}", symbol, data, e);
            return null;
        }
    }
}
