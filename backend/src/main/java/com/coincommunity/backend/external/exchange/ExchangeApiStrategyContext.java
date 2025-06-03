package com.coincommunity.backend.external.exchange;

import com.coincommunity.backend.dto.ExchangePriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
    private final Executor asyncExecutor = Executors.newFixedThreadPool(10);
    
    /**
     * 초기화 시 등록된 전략들 확인
     */
    @PostConstruct
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
        log.info("거래소별 코인 가격 조회 요청 - 거래소: {}, 심볼: {}", exchangeName, symbol);
        
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent()) {
            try {
                ExchangePriceDto price = strategy.get().getCoinPrice(symbol);
                if (price != null) {
                    log.debug("거래소 {} 에서 {} 가격 조회 성공", exchangeName, symbol);
                    return Optional.of(price);
                }
            } catch (Exception e) {
                log.error("거래소 {} 에서 {} 가격 조회 실패", exchangeName, symbol, e);
            }
        } else {
            log.warn("지원하지 않는 거래소: {}", exchangeName);
        }
        
        return Optional.empty();
    }

    /**
     * 모든 거래소에서 특정 코인 가격 조회 (병렬 처리)
     */
    public List<ExchangePriceDto> getAllExchangePrices(String symbol) {
        log.info("전체 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> strategies = strategyFactory.getAllStrategies();
        log.info("사용 가능한 전략 수: {}", strategies.size());
        
        for (ExchangeApiStrategy strategy : strategies) {
            log.info("전략 시도: {} ({}), 건강상태: {}", 
                strategy.getExchangeName(), 
                strategy.getExchangeType(), 
                strategy.isHealthy());
        }
        
        // 병렬로 모든 거래소에서 가격 조회
        List<CompletableFuture<ExchangePriceDto>> futures = strategies.stream()
            .map(strategy -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("거래소 {} 에서 {} 가격 조회 시작", strategy.getExchangeName(), symbol);
                    ExchangePriceDto result = strategy.getCoinPrice(symbol);
                    if (result != null) {
                        log.info("거래소 {} 에서 {} 가격 조회 성공: {}", 
                            strategy.getExchangeName(), symbol, result.getCurrentPrice());
                    } else {
                        log.warn("거래소 {} 에서 {} 가격 조회 결과가 null", strategy.getExchangeName(), symbol);
                    }
                    return result;
                } catch (Exception e) {
                    log.error("거래소 {} 에서 {} 가격 조회 실패", strategy.getExchangeName(), symbol, e);
                    return null;
                }
            }, asyncExecutor))
            .collect(Collectors.toList());
        
        // 모든 비동기 작업 완료 대기
        List<ExchangePriceDto> prices = futures.stream()
            .map(CompletableFuture::join)
            .filter(price -> price != null)
            .collect(Collectors.toList());
        
        log.info("전체 거래소 가격 조회 완료 - 심볼: {}, 조회된 거래소 수: {}/{}", symbol, prices.size(), strategies.size());
        
        // 가격 순으로 정렬 (높은 가격부터)
        return prices.stream()
            .sorted((a, b) -> b.getCurrentPrice().compareTo(a.getCurrentPrice()))
            .collect(Collectors.toList());
    }

    /**
     * 국내 거래소에서 특정 코인 가격 조회
     */
    public List<ExchangePriceDto> getDomesticExchangePrices(String symbol) {
        log.info("국내 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> domesticStrategies = strategyFactory.getDomesticStrategies();
        
        return getExchangePricesFromStrategies(domesticStrategies, symbol);
    }

    /**
     * 해외 거래소에서 특정 코인 가격 조회
     */
    public List<ExchangePriceDto> getForeignExchangePrices(String symbol) {
        log.info("해외 거래소 코인 가격 조회 요청 - 심볼: {}", symbol);
        
        List<ExchangeApiStrategy> foreignStrategies = strategyFactory.getForeignStrategies();
        
        return getExchangePricesFromStrategies(foreignStrategies, symbol);
    }

    /**
     * 특정 거래소에서 모든 코인 가격 조회
     */
    public List<ExchangePriceDto> getAllCoinPrices(String exchangeName) {
        log.info("거래소별 전체 코인 가격 조회 요청 - 거래소: {}", exchangeName);
        
        Optional<ExchangeApiStrategy> strategy = strategyFactory.getStrategy(exchangeName);
        
        if (strategy.isPresent()) {
            try {
                List<ExchangePriceDto> prices = strategy.get().getAllCoinPrices();
                log.info("거래소 {} 전체 코인 가격 조회 완료 - 코인 수: {}", exchangeName, prices.size());
                return prices;
            } catch (Exception e) {
                log.error("거래소 {} 전체 코인 가격 조회 실패", exchangeName, e);
            }
        } else {
            log.warn("지원하지 않는 거래소: {}", exchangeName);
        }
        
        return new ArrayList<>();
    }

    /**
     * 시가총액 상위 코인 조회 (모든 거래소 통합)
     */
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        log.info("시가총액 상위 코인 조회 요청 - 개수: {}", limit);
        
        // CoinGecko를 우선적으로 사용 (시가총액 데이터가 가장 정확)
        Optional<ExchangeApiStrategy> coinGeckoStrategy = strategyFactory.getStrategy("COINGECKO");
        
        if (coinGeckoStrategy.isPresent()) {
            try {
                List<ExchangePriceDto> topCoins = coinGeckoStrategy.get().getTopCoinsByMarketCap(limit);
                log.info("CoinGecko에서 시가총액 상위 코인 조회 완료 - 코인 수: {}", topCoins.size());
                return topCoins;
            } catch (Exception e) {
                log.error("CoinGecko에서 시가총액 상위 코인 조회 실패", e);
            }
        }
        
        // CoinGecko 실패 시 다른 거래소에서 조회
        List<ExchangeApiStrategy> strategies = strategyFactory.getAllStrategies();
        
        for (ExchangeApiStrategy strategy : strategies) {
            try {
                List<ExchangePriceDto> topCoins = strategy.getTopCoinsByMarketCap(limit);
                if (!topCoins.isEmpty()) {
                    log.info("거래소 {}에서 상위 코인 조회 완료 - 코인 수: {}", strategy.getExchangeName(), topCoins.size());
                    return topCoins;
                }
            } catch (Exception e) {
                log.error("거래소 {}에서 상위 코인 조회 실패", strategy.getExchangeName(), e);
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
     * 전략 목록에서 코인 가격 조회 (병렬 처리)
     */
    private List<ExchangePriceDto> getExchangePricesFromStrategies(List<ExchangeApiStrategy> strategies, String symbol) {
        List<CompletableFuture<ExchangePriceDto>> futures = strategies.stream()
            .map(strategy -> CompletableFuture.supplyAsync(() -> {
                try {
                    return strategy.getCoinPrice(symbol);
                } catch (Exception e) {
                    log.error("거래소 {} 에서 {} 가격 조회 실패", strategy.getExchangeName(), symbol, e);
                    return null;
                }
            }, asyncExecutor))
            .collect(Collectors.toList());
        
        return futures.stream()
            .map(CompletableFuture::join)
            .filter(price -> price != null)
            .sorted((a, b) -> b.getCurrentPrice().compareTo(a.getCurrentPrice()))
            .collect(Collectors.toList());
    }
}
