package com.coincommunity.backend.external.exchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 거래소 API 전략 팩토리
 * 거래소별 API 전략을 관리하고 제공하는 팩토리 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeApiStrategyFactory {
    
    private final List<ExchangeApiStrategy> exchangeStrategies;
    
    /**
     * 거래소 이름으로 전략 조회
     */
    public Optional<ExchangeApiStrategy> getStrategy(String exchangeName) {
        return exchangeStrategies.stream()
                .filter(strategy -> strategy.getExchangeName().equalsIgnoreCase(exchangeName))
                .findFirst();
    }
    
    /**
     * 모든 전략 조회
     */
    public List<ExchangeApiStrategy> getAllStrategies() {
        return exchangeStrategies;
    }
    
    /**
     * 국내 거래소 전략들 조회
     */
    public List<ExchangeApiStrategy> getDomesticStrategies() {
        return exchangeStrategies.stream()
                .filter(strategy -> strategy.getExchangeType() == ExchangeApiStrategy.ExchangeType.DOMESTIC)
                .collect(Collectors.toList());
    }
    
    /**
     * 해외 거래소 전략들 조회
     */
    public List<ExchangeApiStrategy> getForeignStrategies() {
        return exchangeStrategies.stream()
                .filter(strategy -> strategy.getExchangeType() == ExchangeApiStrategy.ExchangeType.FOREIGN)
                .collect(Collectors.toList());
    }
    
    /**
     * 건강한 상태의 전략들 조회
     */
    public List<ExchangeApiStrategy> getHealthyStrategies() {
        return exchangeStrategies.stream()
                .filter(ExchangeApiStrategy::isHealthy)
                .collect(Collectors.toList());
    }
    
    /**
     * 거래소별 전략 맵 조회
     */
    public Map<String, ExchangeApiStrategy> getStrategyMap() {
        return exchangeStrategies.stream()
                .collect(Collectors.toMap(
                    ExchangeApiStrategy::getExchangeName,
                    strategy -> strategy,
                    (existing, replacement) -> existing
                ));
    }
}
