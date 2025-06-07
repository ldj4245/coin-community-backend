package com.coincommunity.backend.scheduler;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.entity.CoinPrice;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategyContext;
import com.coincommunity.backend.service.CoinPriceService;
import com.coincommunity.backend.service.MajorCoinService;
import com.coincommunity.backend.websocket.CoinPriceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * 코인 가격 정보를 주기적으로 업데이트하는 스케줄러 (성능 최적화)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinPriceScheduler {
    
    private final ExchangeApiStrategyContext exchangeApiStrategyContext;
    private final CoinPriceService coinPriceService;
    private final CoinPriceWebSocketHandler coinPriceWebSocketHandler;
    private final MajorCoinService majorCoinService;

    @Value("${scheduler.coin-price.enable-domestic:true}")
    private boolean enableDomestic;

    @Value("${scheduler.coin-price.enable-foreign:true}")
    private boolean enableForeign;

    // 코인게코 거래소 이름
    private static final String COINGECKO_EXCHANGE = "COINGECKO";
    
    /**
     * 3분마다 코인 가격 정보를 업데이트합니다. (주기 조정으로 API 부하 감소)
     * 병렬 처리를 통한 성능 최적화 적용
     */
    @Scheduled(fixedRate = 180000) // 3분으로 변경
    public void updateCoinPrices() {
        try {
            log.debug("코인 가격 정보 업데이트 시작");
            List<CoinPrice> allCoinPrices = new ArrayList<>();

            // 병렬로 데이터 수집
            List<ExchangePriceDto> allPrices = collectAllPricesInParallel();

            // 데이터 변환 및 저장
            if (!allPrices.isEmpty()) {
                allCoinPrices = convertToCoinPrices(allPrices);

                // 통계 로깅 (INFO 레벨 유지하되 간소화)
                logDataStatistics(allCoinPrices);

                // 데이터 저장 및 웹소켓 알림
                List<CoinPrice> savedCoinPrices = coinPriceService.saveAllCoinPrices(allCoinPrices);
                
                // WebSocket을 통해 클라이언트에게 실시간 업데이트 전송 (배치 처리)
                sendBatchWebSocketUpdates(savedCoinPrices);
                
                log.info("코인 가격 정보 업데이트 완료: {}개 코인", savedCoinPrices.size());
            } else {
                log.warn("업데이트할 코인 가격 정보가 없습니다.");
            }
        } catch (Exception e) {
            log.error("코인 가격 정보 업데이트 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 병렬로 모든 거래소에서 데이터 수집
     */
    private List<ExchangePriceDto> collectAllPricesInParallel() {
        List<CompletableFuture<List<ExchangePriceDto>>> futures = new ArrayList<>();
        
        // 1. 국내 거래소 데이터 병렬 수집
        if (enableDomestic) {
            String[] domesticExchanges = {"UPBIT", "BITHUMB", "COINONE", "KORBIT"};
            
            for (String exchange : domesticExchanges) {
                CompletableFuture<List<ExchangePriceDto>> future = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            List<ExchangePriceDto> prices = exchangeApiStrategyContext.getAllCoinPrices(exchange);
                            log.debug("{}에서 {}개 코인 데이터 수집됨", exchange, prices.size());
                            return prices;
                        } catch (Exception e) {
                            log.error("{} 데이터 수집 중 오류 발생", exchange, e);
                            return new ArrayList<ExchangePriceDto>();
                        }
                    })
                    .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
                
                futures.add(future);
            }
        }
        
        // 2. CoinGecko 데이터 수집 (시가총액 상위 100개)
        if (enableForeign) {
            CompletableFuture<List<ExchangePriceDto>> coinGeckoFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        List<ExchangePriceDto> coingeckoPrices = exchangeApiStrategyContext.getTopCoinsByMarketCap(100);
                        
                        if (coingeckoPrices.isEmpty()) {
                            log.warn("코인게코에서 시가총액 상위 코인 정보를 가져오지 못했습니다. fallback 시도");
                            coingeckoPrices = exchangeApiStrategyContext.getAllCoinPrices(COINGECKO_EXCHANGE);
                        }
                        
                        // 거래소 이름 정규화
                        coingeckoPrices.forEach(price -> {
                            if (!COINGECKO_EXCHANGE.equals(price.getExchangeName())) {
                                price.setExchangeName(COINGECKO_EXCHANGE);
                                price.setExchangeKoreanName("코인게코");
                            }
                        });
                        
                        log.debug("코인게코에서 {}개 코인 데이터 수집됨", coingeckoPrices.size());
                        return coingeckoPrices;
                    } catch (Exception e) {
                        log.error("코인게코 API 호출 중 오류 발생", e);
                        return new ArrayList<ExchangePriceDto>();
                    }
                })
                .orTimeout(60, java.util.concurrent.TimeUnit.SECONDS); // CoinGecko는 더 긴 타임아웃
            
            futures.add(coinGeckoFuture);
        }
        
        // 모든 Future 완료 대기 및 결과 수집
        List<ExchangePriceDto> allPrices = new ArrayList<>();
        
        for (CompletableFuture<List<ExchangePriceDto>> future : futures) {
            try {
                List<ExchangePriceDto> prices = future.join();
                allPrices.addAll(prices);
            } catch (CompletionException e) {
                log.error("데이터 수집 중 타임아웃 또는 오류 발생", e.getCause());
            }
        }
        
        log.info("총 {}개 코인 데이터 수집 완료", allPrices.size());
        return allPrices;
    }
    
    /**
     * 데이터 통계 로깅
     */
    private void logDataStatistics(List<CoinPrice> allCoinPrices) {
        log.info("변환된 코인 데이터: 총 {}개", allCoinPrices.size());
        
        Map<String, Long> exchangeStats = allCoinPrices.stream()
            .collect(Collectors.groupingBy(CoinPrice::getExchange, Collectors.counting()));
        
        exchangeStats.forEach((exchange, count) -> 
            log.info("{} 거래소 데이터: {}개", exchange, count));
    }
    
    /**
     * 배치 WebSocket 업데이트 전송
     */
    private void sendBatchWebSocketUpdates(List<CoinPrice> savedCoinPrices) {
        try {
            // 주요 코인만 실시간 업데이트 (성능 최적화)
            List<CoinPrice> majorCoinPrices = savedCoinPrices.stream()
                .filter(cp -> majorCoinService.isMajorCoin(cp.getCoinId()))
                .collect(Collectors.toList());
            
            for (CoinPrice coinPrice : majorCoinPrices) {
                coinPriceWebSocketHandler.sendCoinPriceUpdate(coinPrice);
            }
            
            log.debug("주요 코인 {}개에 대한 WebSocket 업데이트 전송 완료", majorCoinPrices.size());
        } catch (Exception e) {
            log.error("WebSocket 업데이트 전송 중 오류 발생", e);
        }
    }
    
    /**
     * 5분마다 시가총액 기준 상위 코인 정보를 업데이트합니다. (캐시 갱신용)
     */
    @Scheduled(fixedRate = 300000)
    public void updateTopMarketCapCoins() {
        if (!enableForeign) {
            return;
        }
        
        try {
            log.debug("시가총액 상위 코인 정보 업데이트 시작");
            
            // 비동기로 처리하여 메인 스케줄러에 영향 주지 않음
            CompletableFuture.runAsync(() -> {
                try {
                    List<ExchangePriceDto> topCoins = exchangeApiStrategyContext.getTopCoinsByMarketCap(100);
                    
                    if (!topCoins.isEmpty()) {
                        List<CoinPrice> coinPrices = convertToCoinPrices(topCoins);
                        coinPriceService.saveAllCoinPrices(coinPrices);
                        log.debug("시가총액 상위 코인 정보 업데이트 완료: {}개 코인", topCoins.size());
                    }
                } catch (Exception e) {
                    log.error("시가총액 상위 코인 정보 업데이트 중 오류 발생", e);
                }
            });
            
        } catch (Exception e) {
            log.error("시가총액 상위 코인 업데이트 스케줄러 시작 중 오류 발생", e);
        }
    }
    
    /**
     * ExchangePriceDto 리스트를 CoinPrice 리스트로 변환 (최적화)
     */
    private List<CoinPrice> convertToCoinPrices(List<ExchangePriceDto> prices) {
        if (prices == null || prices.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDateTime now = LocalDateTime.now();

        // 중복 제거를 위한 Map (Symbol-Exchange를 키로 사용)
        Map<String, ExchangePriceDto> uniquePrices = new HashMap<>();

        // 중복 데이터 제거 - 동일한 코인과 거래소 조합이 있을 경우 마지막 데이터만 유지
        for (ExchangePriceDto price : prices) {
            if (price != null && price.getSymbol() != null && price.getExchangeName() != null) {
                String key = price.getSymbol() + "-" + price.getExchangeName();
                uniquePrices.put(key, price);
            }
        }

        log.debug("총 코인 데이터: {}, 중복 제거 후: {}", prices.size(), uniquePrices.size());

        // 스트림을 사용한 효율적인 변환
        return uniquePrices.values().stream()
            .map(this::convertToCoinPrice)
            .filter(coinPrice -> coinPrice != null)
            .peek(coinPrice -> coinPrice.setLastUpdated(now))
            .collect(Collectors.toList());
    }
    
    /**
     * ExchangePriceDto를 CoinPrice로 변환 (유효성 검사 강화)
     */
    private CoinPrice convertToCoinPrice(ExchangePriceDto dto) {
        try {
            // 필수 필드 검증
            if (dto == null) {
                return null;
            }
            
            if (dto.getSymbol() == null || dto.getSymbol().trim().isEmpty()) {
                log.debug("코인 심볼이 비어있어 변환을 건너뜁니다");
                return null;
            }

            if (dto.getExchangeName() == null || dto.getExchangeName().trim().isEmpty()) {
                log.debug("거래소 이름이 비어있어 변환을 건너뜁니다. 코인: {}", dto.getSymbol());
                return null;
            }

            if (dto.getCurrentPrice() == null || dto.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("현재 가격이 유효하지 않아 변환을 건너뜁니다. 코인: {}, 거래소: {}", 
                    dto.getSymbol(), dto.getExchangeName());
                return null;
            }

            CoinPrice coinPrice = new CoinPrice();
            coinPrice.setCoinId(dto.getSymbol().trim().toUpperCase());
            coinPrice.setExchange(dto.getExchangeName().trim());

            // 한글 이름 설정 (없으면 영문 이름으로 대체)
            coinPrice.setKoreanName(dto.getKoreanName() != null && !dto.getKoreanName().trim().isEmpty() 
                ? dto.getKoreanName().trim() : dto.getSymbol().trim());

            coinPrice.setEnglishName(dto.getSymbol().trim());
            coinPrice.setCurrentPrice(dto.getCurrentPrice());
            
            // null 안전 처리
            coinPrice.setPriceChangePercent(dto.getChangeRate() != null ? dto.getChangeRate() : BigDecimal.ZERO);
            coinPrice.setVolume24h(dto.getVolume24h() != null ? dto.getVolume24h() : BigDecimal.ZERO);
            coinPrice.setHighPrice24h(dto.getHighPrice24h());
            coinPrice.setLowPrice24h(dto.getLowPrice24h());

            // 시가총액 설정
            coinPrice.setMarketCap(dto.getTradeValue24h() != null ? dto.getTradeValue24h() : BigDecimal.ZERO);

            return coinPrice;
        } catch (Exception e) {
            log.error("ExchangePriceDto를 CoinPrice로 변환 실패: {}", 
                    dto != null ? dto.getSymbol() : "null", e);
            return null;
        }
    }
}
