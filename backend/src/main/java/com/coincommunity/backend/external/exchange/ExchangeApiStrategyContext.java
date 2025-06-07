package com.coincommunity.backend.external.exchange;

import com.coincommunity.backend.dto.ExchangePriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 거래소 API 전략 컨텍스트
 * 모든 거래소 API 전략을 통합하여 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeApiStrategyContext {
    
    private final ExchangeApiStrategyFactory strategyFactory;
    
    // 성능 최적화를 위한 전용 스레드 풀
    private Executor asyncExecutor;
    
    // 타임아웃 설정 (초)
    private static final int API_TIMEOUT_SECONDS = 10;
    private static final int BATCH_TIMEOUT_SECONDS = 30;
    
    @PostConstruct
    public void init() {
        // 더 효율적인 스레드 풀 설정
        this.asyncExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "exchange-api-worker");
            t.setDaemon(true);
            return t;
        });
        
        logRegisteredStrategies();
    }
    
    @PreDestroy
    public void cleanup() {
        if (asyncExecutor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) asyncExecutor).shutdown();
        }
    }

    /**
     * 초기화 시 등록된 전략들 확인
     */
    public void logRegisteredStrategies() {
        List<ExchangeApiStrategy> allStrategies = strategyFactory.getAllStrategies();
        log.info("=== 등록된 거래소 API 전략 목록 ===");
        log.info("총 {} 개의 전략이 등록되었습니다", allStrategies.size());
        
        for (ExchangeApiStrategy strategy : allStrategies) {
            log.info("거래소: {}, 타입: {}, 건강상태: {}", 
                strategy.getExchangeName(), 
                strategy.getExchangeType(),
                strategy.isHealthy());
        }
        log.info("===============================");
    }

    /**
     * 특정 거래소에서 코인 가격 조회
     */
    public Optional<ExchangePriceDto> getCoinPrice(String exchangeName, String symbol) {
        log.debug("거래소별 코인 가격 조회 요청 - 거래소: {}, 심볼: {}", exchangeName, symbol);
        
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent()) {
            try {
                // 타임아웃 적용
                CompletableFuture<ExchangePriceDto> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return strategy.get().getCoinPrice(symbol);
                    } catch (Exception e) {
                        log.error("거래소 {} 에서 {} 가격 조회 실패", exchangeName, symbol, e);
                        return null;
                    }
                }, asyncExecutor);
                
                ExchangePriceDto price = future.get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (price != null) {
                    log.debug("거래소 {} 에서 {} 가격 조회 성공", exchangeName, symbol);
                    return Optional.of(price);
                }
            } catch (Exception e) {
                log.error("거래소 {} 에서 {} 가격 조회 타임아웃 또는 오류", exchangeName, symbol, e);
            }
        } else {
            log.warn("지원하지 않는 거래소: {}", exchangeName);
        }
        
        return Optional.empty();
    }

    /**
     * 모든 거래소에서 특정 코인 가격 조회 (병렬 처리 최적화)
     */
    public List<ExchangePriceDto> getAllExchangePrices(String symbol) {
        log.debug("전체 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> strategies = strategyFactory.getHealthyStrategies(); // 건강한 거래소만 조회
        if (strategies.isEmpty()) {
            log.warn("사용 가능한 건강한 거래소가 없습니다");
            return new ArrayList<>();
        }
        
        log.debug("사용 가능한 건강한 전략 수: {}", strategies.size());
        
        // 병렬로 모든 거래소에서 가격 조회 (타임아웃 적용)
        List<CompletableFuture<ExchangePriceDto>> futures = strategies.stream()
            .map(strategy -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("거래소 {} 에서 {} 가격 조회 시작", strategy.getExchangeName(), symbol);
                    ExchangePriceDto result = strategy.getCoinPrice(symbol);
                    if (result != null) {
                        log.debug("거래소 {} 에서 {} 가격 조회 성공: {}", 
                            strategy.getExchangeName(), symbol, result.getCurrentPrice());
                    }
                    return result;
                } catch (Exception e) {
                    log.debug("거래소 {} 에서 {} 가격 조회 실패", strategy.getExchangeName(), symbol, e);
                    return null;
                }
            }, asyncExecutor).orTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .collect(Collectors.toList());
        
        // 모든 비동기 작업 완료 대기 (배치 타임아웃 적용)
        List<ExchangePriceDto> prices = new ArrayList<>();
        for (CompletableFuture<ExchangePriceDto> future : futures) {
            try {
                ExchangePriceDto price = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (price != null) {
                    prices.add(price);
                }
            } catch (Exception e) {
                log.debug("거래소 가격 조회 타임아웃 또는 오류", e);
            }
        }
        
        log.debug("전체 거래소 가격 조회 완료 - 심볼: {}, 조회된 거래소 수: {}/{}", symbol, prices.size(), strategies.size());
        
        // 가격 순으로 정렬 (높은 가격부터)
        return prices.stream()
            .sorted((a, b) -> b.getCurrentPrice().compareTo(a.getCurrentPrice()))
            .collect(Collectors.toList());
    }

    /**
     * 국내 거래소에서 특정 코인 가격 조회
     */
    public List<ExchangePriceDto> getDomesticExchangePrices(String symbol) {
        log.debug("국내 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> domesticStrategies = strategyFactory.getDomesticStrategies()
            .stream()
            .filter(ExchangeApiStrategy::isHealthy)
            .collect(Collectors.toList());
        
        return getExchangePricesFromStrategies(domesticStrategies, symbol);
    }

    /**
     * 해외 거래소에서 특정 코인 가격 조회
     */
    public List<ExchangePriceDto> getForeignExchangePrices(String symbol) {
        log.debug("해외 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> foreignStrategies = strategyFactory.getForeignStrategies()
            .stream()
            .filter(ExchangeApiStrategy::isHealthy)
            .collect(Collectors.toList());
        
        return getExchangePricesFromStrategies(foreignStrategies, symbol);
    }

    /**
     * 특정 거래소에서 모든 코인 가격 조회
     */
    public List<ExchangePriceDto> getAllCoinPrices(String exchangeName) {
        log.debug("거래소별 전체 코인 가격 조회 요청 - 거래소: {}", exchangeName);
        
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent() && strategy.get().isHealthy()) {
            try {
                // 타임아웃 적용
                CompletableFuture<List<ExchangePriceDto>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return strategy.get().getAllCoinPrices();
                    } catch (Exception e) {
                        log.error("거래소 {} 전체 코인 가격 조회 실패", exchangeName, e);
                        return new ArrayList<ExchangePriceDto>();
                    }
                }, asyncExecutor);
                
                List<ExchangePriceDto> prices = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.debug("거래소 {} 전체 코인 가격 조회 완료 - 코인 수: {}", exchangeName, prices.size());
                return prices;
                
            } catch (Exception e) {
                log.error("거래소 {} 전체 코인 가격 조회 타임아웃 또는 오류", exchangeName, e);
            }
        } else {
            log.warn("지원하지 않거나 건강하지 않은 거래소: {}", exchangeName);
        }
        
        return new ArrayList<>();
    }

    /**
     * 시가총액 상위 코인 조회 (모든 거래소 통합)
     */
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        log.debug("시가총액 상위 코인 조회 요청 - 개수: {}", limit);
        
        // CoinGecko를 우선적으로 사용 (시가총액 데이터가 가장 정확)
        Optional<ExchangeApiStrategy> coinGeckoStrategy = strategyFactory.getStrategy("COINGECKO");
        
        if (coinGeckoStrategy.isPresent() && coinGeckoStrategy.get().isHealthy()) {
            try {
                // 타임아웃 적용
                CompletableFuture<List<ExchangePriceDto>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return coinGeckoStrategy.get().getTopCoinsByMarketCap(limit);
                    } catch (Exception e) {
                        log.error("CoinGecko에서 시가총액 상위 코인 조회 실패", e);
                        return new ArrayList<ExchangePriceDto>();
                    }
                }, asyncExecutor);
                
                List<ExchangePriceDto> topCoins = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!topCoins.isEmpty()) {
                    log.debug("CoinGecko에서 시가총액 상위 코인 조회 완료 - 코인 수: {}", topCoins.size());
                    return topCoins;
                }
            } catch (Exception e) {
                log.error("CoinGecko에서 시가총액 상위 코인 조회 타임아웃", e);
            }
        }
        
        // CoinGecko 실패 시 다른 거래소에서 조회
        List<ExchangeApiStrategy> strategies = strategyFactory.getHealthyStrategies();
        
        for (ExchangeApiStrategy strategy : strategies) {
            if ("COINGECKO".equals(strategy.getExchangeName())) {
                continue; // 이미 시도했으므로 스킵
            }
            
            try {
                CompletableFuture<List<ExchangePriceDto>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return strategy.getTopCoinsByMarketCap(limit);
                    } catch (Exception e) {
                        log.debug("거래소 {}에서 상위 코인 조회 실패", strategy.getExchangeName(), e);
                        return new ArrayList<ExchangePriceDto>();
                    }
                }, asyncExecutor);
                
                List<ExchangePriceDto> topCoins = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!topCoins.isEmpty()) {
                    log.debug("거래소 {}에서 상위 코인 조회 완료 - 코인 수: {}", strategy.getExchangeName(), topCoins.size());
                    return topCoins;
                }
            } catch (Exception e) {
                log.debug("거래소 {}에서 상위 코인 조회 타임아웃", strategy.getExchangeName(), e);
            }
        }
        
        log.warn("모든 거래소에서 시가총액 상위 코인 조회 실패");
        return new ArrayList<>();
    }

    /**
     * 거래소별 지원 코인 목록 조회
     */
    public List<String> getSupportedCoins(String exchangeName) {
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent()) {
            return strategy.get().getSupportedCoins();
        }
        
        return new ArrayList<>();
    }

    /**
     * 모든 거래소에서 지원하는 코인 목록 조회 (합집합)
     */
    public List<String> getAllSupportedCoins() {
        return strategyFactory.getAllStrategies().stream()
            .flatMap(strategy -> strategy.getSupportedCoins().stream())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * 거래소 상태 확인
     */
    public boolean isExchangeHealthy(String exchangeName) {
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent()) {
            return strategy.get().isHealthy();
        }
        
        return false;
    }

    /**
     * 모든 거래소 상태 확인
     */
    public List<String> getHealthyExchanges() {
        return strategyFactory.getHealthyStrategies().stream()
            .map(ExchangeApiStrategy::getExchangeName)
            .collect(Collectors.toList());
    }

    /**
     * 지원하는 거래소 목록 조회
     */
    public List<String> getSupportedExchanges() {
        return strategyFactory.getAllStrategies().stream()
            .map(ExchangeApiStrategy::getExchangeName)
            .collect(Collectors.toList());
    }

    /**
     * 전략 목록에서 코인 가격 조회 (병렬 처리 최적화)
     */
    private List<ExchangePriceDto> getExchangePricesFromStrategies(List<ExchangeApiStrategy> strategies, String symbol) {
        if (strategies.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<CompletableFuture<ExchangePriceDto>> futures = strategies.stream()
            .map(strategy -> CompletableFuture.supplyAsync(() -> {
                try {
                    return strategy.getCoinPrice(symbol);
                } catch (Exception e) {
                    log.debug("거래소 {} 에서 {} 가격 조회 실패", strategy.getExchangeName(), symbol, e);
                    return null;
                }
            }, asyncExecutor).orTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .collect(Collectors.toList());
        
        List<ExchangePriceDto> results = new ArrayList<>();
        for (CompletableFuture<ExchangePriceDto> future : futures) {
            try {
                ExchangePriceDto price = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (price != null) {
                    results.add(price);
                }
            } catch (Exception e) {
                log.debug("거래소 가격 조회 타임아웃", e);
            }
        }
        
        return results.stream()
            .sorted((a, b) -> b.getCurrentPrice().compareTo(a.getCurrentPrice()))
            .collect(Collectors.toList());
    }
}
