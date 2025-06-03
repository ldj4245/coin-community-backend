package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.ExchangeComparisonDto;
import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 거래소별 시세 서비스 (Strategy Pattern 적용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangePriceService {

    private final ExchangeApiStrategyContext strategyContext;

    /**
     * 특정 코인의 거래소별 시세 조회
     */
    @Cacheable(value = "exchangePrices", key = "#symbol")
    public List<ExchangePriceDto> getExchangePrices(String symbol) {
        log.info("거래소별 시세 조회 시작 - 심볼: {}", symbol);

        List<ExchangePriceDto> prices = strategyContext.getAllExchangePrices(symbol);
        
        log.info("거래소별 시세 조회 완료 - 심볼: {}, 조회된 거래소 수: {}", symbol, prices.size());
        
        return prices;
    }

    /**
     * 국내 거래소 시세 조회
     */
    @Cacheable(value = "domesticExchangePrices", key = "#symbol")
    public List<ExchangePriceDto> getDomesticExchangePrices(String symbol) {
        log.info("국내 거래소 시세 조회 시작 - 심볼: {}", symbol);
        
        return strategyContext.getDomesticExchangePrices(symbol);
    }

    /**
     * 해외 거래소 시세 조회
     */
    @Cacheable(value = "foreignExchangePrices", key = "#symbol")
    public List<ExchangePriceDto> getForeignExchangePrices(String symbol) {
        log.info("해외 거래소 시세 조회 시작 - 심볼: {}", symbol);
        
        return strategyContext.getForeignExchangePrices(symbol);
    }

    /**
     * 특정 거래소의 모든 코인 가격 조회
     */
    @Cacheable(value = "allCoinPrices", key = "#exchangeName")
    public List<ExchangePriceDto> getAllCoinPricesByExchange(String exchangeName) {
        log.info("거래소별 전체 코인 가격 조회 시작 - 거래소: {}", exchangeName);
        
        return strategyContext.getAllCoinPrices(exchangeName);
    }

    /**
     * 거래소별 시세 비교 정보 조회
     */
    @Cacheable(value = "exchangeComparison", key = "#symbol")
    public ExchangeComparisonDto getExchangeComparison(String symbol) {
        log.info("거래소별 시세 비교 조회 시작 - 심볼: {}", symbol);

        try {
            List<ExchangePriceDto> allPrices = getExchangePrices(symbol);
            
            if (allPrices.isEmpty()) {
                log.warn("거래소 시세 정보가 없습니다 - 심볼: {}", symbol);
                return null;
            }

            // 국내/해외 거래소 분리
            List<ExchangePriceDto> domesticPrices = allPrices.stream()
                .filter(price -> price.getExchangeType() == ExchangePriceDto.ExchangeType.DOMESTIC)
                .collect(Collectors.toList());

            List<ExchangePriceDto> foreignPrices = allPrices.stream()
                .filter(price -> price.getExchangeType() == ExchangePriceDto.ExchangeType.FOREIGN)
                .collect(Collectors.toList());

            // 최고가/최저가 계산
            ExchangePriceDto highest = allPrices.stream()
                .max(Comparator.comparing(ExchangePriceDto::getCurrentPrice))
                .orElse(null);

            ExchangePriceDto lowest = allPrices.stream()
                .min(Comparator.comparing(ExchangePriceDto::getCurrentPrice))
                .orElse(null);

            // 가격 차이 및 차이율 계산
            BigDecimal maxPriceDifference = BigDecimal.ZERO;
            BigDecimal maxPriceDifferenceRate = BigDecimal.ZERO;

            if (highest != null && lowest != null) {
                maxPriceDifference = highest.getCurrentPrice().subtract(lowest.getCurrentPrice());
                if (lowest.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                    maxPriceDifferenceRate = maxPriceDifference
                        .divide(lowest.getCurrentPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                }
            }

            return ExchangeComparisonDto.builder()
                .symbol(symbol)
                .totalExchanges(allPrices.size())
                .domesticExchanges(domesticPrices.size())
                .foreignExchanges(foreignPrices.size())
                .exchangePrices(allPrices)
                .highest(createExchangeHighLow(highest))
                .lowest(createExchangeHighLow(lowest))
                .maxPriceDifference(maxPriceDifference)
                .maxPriceDifferenceRate(maxPriceDifferenceRate)
                .averagePrice(calculateAveragePrice(allPrices))
                .medianPrice(calculateMedianPrice(allPrices))
                .standardDeviation(calculateStandardDeviation(allPrices))
                .build();

        } catch (Exception e) {
            log.error("거래소별 시세 비교 조회 실패 - 심볼: {}", symbol, e);
            return null;
        }
    }

    /**
     * 시가총액 상위 코인 조회
     */
    @Cacheable(value = "topCoinsByMarketCap", key = "#limit")
    public List<ExchangePriceDto> getTopCoinsByMarketCap(int limit) {
        log.info("시가총액 상위 코인 조회 시작 - 개수: {}", limit);
        
        return strategyContext.getTopCoinsByMarketCap(limit);
    }

    /**
     * 지원하는 거래소 목록 조회
     */
    public List<String> getSupportedExchanges() {
        return strategyContext.getSupportedExchanges();
    }

    /**
     * 거래소별 지원 코인 목록 조회
     */
    public List<String> getSupportedCoins(String exchangeName) {
        return strategyContext.getSupportedCoins(exchangeName);
    }

    /**
     * 모든 거래소에서 지원하는 코인 목록 조회
     */
    public List<String> getAllSupportedCoins() {
        return strategyContext.getAllSupportedCoins();
    }

    /**
     * 거래소 상태 확인
     */
    public boolean isExchangeHealthy(String exchangeName) {
        return strategyContext.isExchangeHealthy(exchangeName);
    }

    /**
     * 건강한 상태의 거래소 목록 조회
     */
    public List<String> getHealthyExchanges() {
        return strategyContext.getHealthyExchanges();
    }

    /**
     * ExchangeHighLow 객체 생성
     */
    private ExchangeComparisonDto.ExchangeHighLow createExchangeHighLow(ExchangePriceDto priceDto) {
        if (priceDto == null) {
            return null;
        }

        return ExchangeComparisonDto.ExchangeHighLow.builder()
            .exchangeName(priceDto.getExchangeName())
            .exchangeKoreanName(priceDto.getExchangeKoreanName())
            .exchangeType(priceDto.getExchangeType())
            .price(priceDto.getCurrentPrice())
            .changeRate(priceDto.getChangeRate())
            .volume(priceDto.getVolume24h())
            .build();
    }

    /**
     * 평균 가격 계산
     */
    private BigDecimal calculateAveragePrice(List<ExchangePriceDto> prices) {
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = prices.stream()
            .map(ExchangePriceDto::getCurrentPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(prices.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 중간값 계산
     */
    private BigDecimal calculateMedianPrice(List<ExchangePriceDto> prices) {
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sortedPrices = prices.stream()
            .map(ExchangePriceDto::getCurrentPrice)
            .sorted()
            .collect(Collectors.toList());

        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return sortedPrices.get(size / 2 - 1)
                .add(sortedPrices.get(size / 2))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else {
            return sortedPrices.get(size / 2);
        }
    }

    /**
     * 표준편차 계산
     */
    private BigDecimal calculateStandardDeviation(List<ExchangePriceDto> prices) {
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal average = calculateAveragePrice(prices);
        
        BigDecimal sumOfSquares = prices.stream()
            .map(ExchangePriceDto::getCurrentPrice)
            .map(price -> price.subtract(average).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumOfSquares.divide(new BigDecimal(prices.size()), 4, RoundingMode.HALF_UP);
        
        // 제곱근 계산 (BigDecimal에서는 근사치 사용)
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}
