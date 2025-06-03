package com.coincommunity.backend.external.exchange.binance;

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
 * 바이낸스 거래소 API 클라이언트
 * 글로벌 거래소로 USD 기준 가격 정보를 제공합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceApiClient implements ExchangeApiStrategy {

    private final RestTemplate restTemplate;

    @Value("${external.binance.base-url}")
    private String baseUrl;
    
    @Value("${external.binance.version}")
    private String version;

    // API 엔드포인트 상수 정의
    private static final String TICKER_24HR_ENDPOINT = "/ticker/24hr";
    private static final String PING_ENDPOINT = "/ping";
    
    /**
     * 완전한 API URL 생성
     */
    private String buildUrl(String endpoint) {
        return baseUrl + "/" + version + endpoint;
    }

    // USD-KRW 환율 (실제 환경에서는 환율 API에서 가져와야 함)
    private static final BigDecimal USD_EXCHANGE_RATE = new BigDecimal("1350.50");

    // 바이낸스에서 지원하는 주요 코인 목록
    private static final List<String> SUPPORTED_COINS = List.of(
        "BTC", "ETH", "BNB", "XRP", "ADA", "DOT", "LINK", "LTC", "BCH", "SOL",
        "MATIC", "AVAX", "ATOM", "UNI", "FIL", "TRX", "ETC", "XLM", "VET", "ICP"
    );

    // 코인별 한글명 매핑
    private static final Map<String, String> COIN_KOREAN_NAMES = Map.of(
        "BTC", "비트코인",
        "ETH", "이더리움", 
        "BNB", "바이낸스 코인",
        "XRP", "리플",
        "ADA", "에이다",
        "DOT", "폴카닷",
        "LINK", "체인링크",
        "LTC", "라이트코인",
        "BCH", "비트코인캐시",
        "SOL", "솔라나"
    );

    @Override
    public String getExchangeName() {
        return "BINANCE";
    }

    @Override
    public ExchangeApiStrategy.ExchangeType getExchangeType() {
        return ExchangeApiStrategy.ExchangeType.FOREIGN;
    }

    @Override
    public List<String> getSupportedCoins() {
        return new ArrayList<>(SUPPORTED_COINS);
    }

    @Override
    public List<ExchangePriceDto> getAllCoinPrices() {
        log.info("바이낸스 전체 코인 가격 조회 요청");
        
        List<ExchangePriceDto> prices = new ArrayList<>();
        
        try {
            String url = buildUrl(TICKER_24HR_ENDPOINT);
            log.debug("바이낸스 전체 가격 정보 API 호출: {}", url);
            
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            
            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tickerList = (List<Map<String, Object>>) response.getBody();
                
                for (Map<String, Object> ticker : tickerList) {
                    String symbol = ticker.get("symbol").toString();
                    
                    // USDT 페어만 처리하고, 지원하는 코인인지 확인
                    if (symbol.endsWith("USDT")) {
                        String coinSymbol = symbol.replace("USDT", "");
                        if (SUPPORTED_COINS.contains(coinSymbol)) {
                            ExchangePriceDto priceDto = createPriceDto(ticker, coinSymbol);
                            if (priceDto != null) {
                                prices.add(priceDto);
                            }
                        }
                    }
                }
                
                log.info("바이낸스 가격 정보 조회 완료 - 코인 수: {}", prices.size());
            }
            
        } catch (Exception e) {
            log.error("바이낸스 전체 가격 정보 조회 실패", e);
        }
        
        return prices;
    }

    @Override
    public ExchangePriceDto getCoinPrice(String symbol) {
        log.info("바이낸스 개별 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        try {
            String url = buildUrl(TICKER_24HR_ENDPOINT) + "?symbol=" + symbol + "USDT";
            log.debug("바이낸스 API 호출: {}", url);
            
            long startTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.getForEntity(url, Map.class);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                ExchangePriceDto priceDto = createPriceDto(data, symbol);
                if (priceDto != null) {
                    // 응답 시간을 별도로 설정할 수 없으므로 새로운 객체 생성
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
            
        } catch (Exception e) {
            log.error("바이낸스 개별 코인 가격 조회 실패 - 심볼: {}", symbol, e);
        }
        
        return null;
    }

    @Override
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        log.info("바이낸스 시가총액 상위 코인 조회 요청 - 개수: {}", limit);
        
        // 바이낸스 API는 시가총액 순으로 정렬된 데이터를 제공하지 않으므로
        // 전체 데이터를 가져와서 거래량 기준으로 정렬
        List<ExchangePriceDto> allPrices = getAllCoinPrices();
        
        return allPrices.stream()
            .sorted((a, b) -> b.getTradeValue24h().compareTo(a.getTradeValue24h()))
            .limit(limit)
            .toList();
    }

    @Override
    public boolean isHealthy() {
        try {
            String url = buildUrl(PING_ENDPOINT);
            log.debug("바이낸스 헬스체크 API 호출: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            
            log.info("바이낸스 API 상태 확인 - 정상: {}", isHealthy);
            return isHealthy;
            
        } catch (Exception e) {
            log.error("바이낸스 API 헬스체크 실패", e);
            return false;
        }
    }

    /**
     * 바이낸스 API 응답 데이터를 ExchangePriceDto로 변환
     */
    private ExchangePriceDto createPriceDto(Map<String, Object> data, String symbol) {
        try {
            BigDecimal priceUsd = new BigDecimal(data.get("lastPrice").toString());
            BigDecimal priceKrw = priceUsd.multiply(USD_EXCHANGE_RATE);
            BigDecimal changeRate = new BigDecimal(data.get("priceChangePercent").toString());
            BigDecimal highPriceUsd = new BigDecimal(data.get("highPrice").toString());
            BigDecimal lowPriceUsd = new BigDecimal(data.get("lowPrice").toString());
            BigDecimal bidPriceUsd = new BigDecimal(data.get("bidPrice").toString());
            BigDecimal askPriceUsd = new BigDecimal(data.get("askPrice").toString());
            
            return ExchangePriceDto.builder()
                .exchangeName("BINANCE")
                .exchangeKoreanName("바이낸스")
                .exchangeType(ExchangePriceDto.ExchangeType.FOREIGN)
                .symbol(symbol)
                .koreanName(COIN_KOREAN_NAMES.getOrDefault(symbol, symbol))
                .currentPrice(priceKrw)
                .changePrice(priceUsd.multiply(changeRate).divide(new BigDecimal("100"), RoundingMode.HALF_UP).multiply(USD_EXCHANGE_RATE))
                .changeRate(changeRate)
                .highPrice24h(highPriceUsd.multiply(USD_EXCHANGE_RATE))
                .lowPrice24h(lowPriceUsd.multiply(USD_EXCHANGE_RATE))
                .volume24h(new BigDecimal(data.get("volume").toString()))
                .tradeValue24h(new BigDecimal(data.get("quoteVolume").toString()).multiply(USD_EXCHANGE_RATE))
                .bidPrice(bidPriceUsd.multiply(USD_EXCHANGE_RATE))
                .askPrice(askPriceUsd.multiply(USD_EXCHANGE_RATE))
                .spread(askPriceUsd.subtract(bidPriceUsd).multiply(USD_EXCHANGE_RATE))
                .spreadRate(calculateSpreadRate(bidPriceUsd, askPriceUsd))
                .status(ExchangePriceDto.TradingStatus.NORMAL)
                .marketWarning(ExchangePriceDto.MarketWarning.NONE)
                .lastUpdated(LocalDateTime.now())
                .priceReliability(98) // 바이낸스는 높은 신뢰도
                .build();
                
        } catch (Exception e) {
            log.error("바이낸스 가격 데이터 변환 실패 - 심볼: {}", symbol, e);
            return null;
        }
    }

    /**
     * 스프레드 비율 계산
     */
    private BigDecimal calculateSpreadRate(BigDecimal bidPrice, BigDecimal askPrice) {
        if (bidPrice.compareTo(BigDecimal.ZERO) > 0) {
            return askPrice.subtract(bidPrice)
                .divide(bidPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
}
