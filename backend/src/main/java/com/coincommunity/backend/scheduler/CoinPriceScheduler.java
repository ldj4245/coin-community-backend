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

/**
 * 코인 가격 정보를 주기적으로 업데이트하는 스케줄러
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
     * 1분마다 코인 가격 정보를 업데이트합니다.
     * 하이브리드 전략:
     * - 주요 코인: 모든 거래소의 데이터 저장
     * - 기타 코인: CoinGecko 데이터만 저장
     */
    @Scheduled(fixedRate = 60000)
    public void updateCoinPrices() {
        try {
            log.debug("코인 가격 정보 업데이트 시작");
            List<CoinPrice> allCoinPrices = new ArrayList<>();

            // 주요 코인 목록 가져오기
            List<ExchangePriceDto> allPrices = new ArrayList<>();

            // 국내 거래소별 데이터 수집 현황 로깅
            log.info("===== 거래소별 데이터 수집 시작 =====");

            // 업비트 데이터 수집
            try {
                List<ExchangePriceDto> upbitPrices = exchangeApiStrategyContext.getAllCoinPrices("UPBIT");
                log.info("업비트에서 {}개 코인 데이터 수집됨", upbitPrices.size());
                allPrices.addAll(upbitPrices);
            } catch (Exception e) {
                log.error("업비트 데이터 수집 중 오류 발생", e);
            }

            // 빗썸 데이터 수집
            try {
                List<ExchangePriceDto> bithumbPrices = exchangeApiStrategyContext.getAllCoinPrices("BITHUMB");
                log.info("빗썸에서 {}개 코인 데이터 수집됨", bithumbPrices.size());
                allPrices.addAll(bithumbPrices);
            } catch (Exception e) {
                log.error("빗썸 데이터 수집 중 오류 발생", e);
            }

            // 코인원 데이터 수집
            try {
                List<ExchangePriceDto> coinonePrices = exchangeApiStrategyContext.getAllCoinPrices("COINONE");
                log.info("코인원에서 {}개 코인 데이터 수집됨", coinonePrices.size());
                allPrices.addAll(coinonePrices);
            } catch (Exception e) {
                log.error("코인원 데이터 수집 중 오류 발생", e);
            }

            // 코빗 데이터 수집
            try {
                List<ExchangePriceDto> korbitPrices = exchangeApiStrategyContext.getAllCoinPrices("KORBIT");
                log.info("코빗에서 {}개 코인 데이터 수집됨", korbitPrices.size());
                allPrices.addAll(korbitPrices);
            } catch (Exception e) {
                log.error("코빗 데이터 수집 중 오류 발생", e);
            }

            // 1. 국내 거래소에서 데이터 수집 (기존 코드는 주석 처리)
            /*
            if (enableDomestic) {
                try {
                    // 주요 코인 포함 상위 50개 코인 가져오기
                    List<ExchangePriceDto> domesticPrices = exchangeApiStrategyContext.getTopCoinsByMarketCap(50);

                    // 하이브리드 전략 적용: 주요 코인은 모든 거래소 데이터 저장, 기타 코인은 필터링
                    for (ExchangePriceDto price : domesticPrices) {
                        String coinId = price.getSymbol();
                        String exchange = price.getExchangeName();

                        // 주요 코인이거나 코인게코 데이터인 경우만 저장
                        if (majorCoinService.isMajorCoin(coinId) || COINGECKO_EXCHANGE.equals(exchange)) {
                            allPrices.add(price);
                        }
                    }

                    log.debug("국내 거래소에서 {}개 코인 가격 정보 수집", domesticPrices.size());
                } catch (Exception e) {
                    log.error("국내 거래소 API 호출 중 오류 발생", e);
                }
            }
            */

            // 2. 해외 거래소에서 데이터 수집
            if (enableForeign) {
                try {
                    // 주요 코인들에 대해 모든 거래소 데이터 수집
                    for (String majorCoin : majorCoinService.getMajorCoins()) {
                        try {
                            List<ExchangePriceDto> foreignPrices = exchangeApiStrategyContext.getForeignExchangePrices(majorCoin);
                            allPrices.addAll(foreignPrices);
                        } catch (Exception e) {
                            log.error("해외 거래소 API에서 {}코인 데이터 수집 중 오류 발생", majorCoin, e);
                        }
                    }

                    log.debug("해외 거래소에서 주요 코인들의 가격 정보 수집 완료");
                } catch (Exception e) {
                    log.error("해외 거래소 API 호출 중 오류 발생", e);
                }
            }

            // 3. CoinGecko에서 모든 코인 데이터 수집 (주요 코인 + 기타 코인)
            try {
                // 코인게코에서 시가총액 상위 100개 코인 정보 가져오기
                List<ExchangePriceDto> coingeckoPrices = exchangeApiStrategyContext.getTopCoinsByMarketCap(100);
                if (coingeckoPrices.isEmpty()) {
                    log.warn("코인게코에서 시가총액 상위 코인 정보를 가져오지 못했습니다. fallback으로 getAllCoinPrices 시도");
                    coingeckoPrices = exchangeApiStrategyContext.getAllCoinPrices(COINGECKO_EXCHANGE);
                }

                // 각 코인이 코인게코 거래소 이름으로 설정되어 있는지 확인
                for (ExchangePriceDto price : coingeckoPrices) {
                    if (!COINGECKO_EXCHANGE.equals(price.getExchangeName())) {
                        price.setExchangeName(COINGECKO_EXCHANGE);
                        price.setExchangeKoreanName("코인게코");
                    }
                }

                log.info("코인게코에서 {}개 코인 데이터 수집됨", coingeckoPrices.size());
                allPrices.addAll(coingeckoPrices);
            } catch (Exception e) {
                log.error("코인게코 API 호출 중 오류 발생", e);
            }

            log.info("===== 거래소별 데이터 수집 완료 =====");

            // 4. 모든 데이터를 CoinPrice 엔티티로 변환
            allCoinPrices = convertToCoinPrices(allPrices);

            // 로그 출력으로 데이터 상태 확인
            log.info("변환된 코인 데이터: 총 {}개", allCoinPrices.size());
            for (String exchange : majorCoinService.getSupportedExchanges()) {
                long count = allCoinPrices.stream()
                    .filter(cp -> exchange.equals(cp.getExchange()))
                    .count();
                log.info("{} 거래소 데이터: {}개", exchange, count);
            }

            // 5. 데이터 저장 및 웹소켓 알림
            if (!allCoinPrices.isEmpty()) {
                List<CoinPrice> savedCoinPrices = coinPriceService.saveAllCoinPrices(allCoinPrices);
                
                // WebSocket을 통해 클라이언트에게 실시간 업데이트 전송
                for (CoinPrice coinPrice : savedCoinPrices) {
                    coinPriceWebSocketHandler.sendCoinPriceUpdate(coinPrice);
                }
                
                log.debug("코인 가격 정보 업데이트 완료: {}개 코인", savedCoinPrices.size());
            } else {
                log.warn("업데이트할 코인 가격 정보가 없습니다.");
            }
        } catch (Exception e) {
            log.error("코인 가격 정보 업데이트 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 5분마다 시가총액 기준 상위 코인 정보를 업데이트합니다.
     */
    @Scheduled(fixedRate = 300000)
    public void updateTopMarketCapCoins() {
        if (!enableForeign) {
            return;
        }
        
        try {
            log.debug("시가총액 상위 코인 정보 업데이트 시작");
            List<ExchangePriceDto> topCoins = exchangeApiStrategyContext.getTopCoinsByMarketCap(100);
            
            if (!topCoins.isEmpty()) {
                List<CoinPrice> coinPrices = convertToCoinPrices(topCoins);
                coinPriceService.saveAllCoinPrices(coinPrices);
                log.debug("시가총액 상위 코인 정보 업데이트 완료: {}개 코인", topCoins.size());
            }
        } catch (Exception e) {
            log.error("시가총액 상위 코인 정보 업데이트 중 오류 발생", e);
        }
    }
    
    /**
     * ExchangePriceDto 리스트를 CoinPrice 리스트로 변환
     */
    private List<CoinPrice> convertToCoinPrices(List<ExchangePriceDto> prices) {
        List<CoinPrice> coinPrices = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 중복 제거를 위한 Map (Symbol-Exchange를 키로 사용)
        Map<String, ExchangePriceDto> uniquePrices = new HashMap<>();

        // 중복 데이터 제거 - 동일한 코인과 거래소 조합이 있을 경우 마지막 데이터만 유지
        for (ExchangePriceDto price : prices) {
            String key = price.getSymbol() + "-" + price.getExchangeName();
            uniquePrices.put(key, price);
        }

        log.info("총 코인 데이터: {}, 중복 제거 후: {}", prices.size(), uniquePrices.size());

        // 중복이 제거된 데이터만 처리
        for (ExchangePriceDto price : uniquePrices.values()) {
            CoinPrice coinPrice = new CoinPrice();
            coinPrice.setCoinId(price.getSymbol());
            coinPrice.setExchange(price.getExchangeName());
            coinPrice.setCurrentPrice(price.getCurrentPrice());
            coinPrice.setPriceChangePercent(price.getChangeRate());
            coinPrice.setVolume24h(price.getVolume24h() != null ? price.getVolume24h() : BigDecimal.ZERO);
            coinPrice.setMarketCap(price.getTradeValue24h() != null ? price.getTradeValue24h() : BigDecimal.ZERO);
            coinPrice.setLastUpdated(now);

            // 필수 필드인 한글명, 영어명 설정
            coinPrice.setKoreanName(price.getKoreanName() != null ? price.getKoreanName() : price.getSymbol());
            coinPrice.setEnglishName(price.getSymbol());

            // 추가 필드 설정
            if (price.getHighPrice24h() != null) {
                coinPrice.setHighPrice24h(price.getHighPrice24h());
            }
            if (price.getLowPrice24h() != null) {
                coinPrice.setLowPrice24h(price.getLowPrice24h());
            }

            coinPrices.add(coinPrice);
        }

        return coinPrices;
    }
    
    /**
     * ExchangePriceDto를 CoinPrice로 변환
     */
    private CoinPrice convertToCoinPrice(ExchangePriceDto dto) {
        try {
            // 필수 필드 검증
            if (dto.getSymbol() == null || dto.getSymbol().isEmpty()) {
                log.warn("코인 심볼이 비어있어 변환을 건너뜁니다: {}", dto);
                return null;
            }

            if (dto.getExchangeName() == null || dto.getExchangeName().isEmpty()) {
                log.warn("거래소 이름이 비어있어 변환을 건너뜁니다. 코인: {}", dto.getSymbol());
                return null;
            }

            if (dto.getCurrentPrice() == null) {
                log.warn("현재 가격이 null이어서 변환을 건너뜁니다. 코인: {}, 거래소: {}", 
                    dto.getSymbol(), dto.getExchangeName());
                return null;
            }

            CoinPrice coinPrice = new CoinPrice();
            coinPrice.setCoinId(dto.getSymbol());
            coinPrice.setExchange(dto.getExchangeName());

            // 한글 이름 설정 (없으면 영문 이름으로 대체)
            if (dto.getKoreanName() != null && !dto.getKoreanName().isEmpty()) {
                coinPrice.setKoreanName(dto.getKoreanName());
            } else {
                coinPrice.setKoreanName(dto.getSymbol());
            }

            coinPrice.setEnglishName(dto.getSymbol());
            coinPrice.setCurrentPrice(dto.getCurrentPrice());
            coinPrice.setPriceChangePercent(dto.getChangeRate());
            coinPrice.setVolume24h(dto.getVolume24h());
            coinPrice.setHighPrice24h(dto.getHighPrice24h());
            coinPrice.setLowPrice24h(dto.getLowPrice24h());

            // 시가총액 설정 (없으면 0으로 설정)
            if (dto.getTradeValue24h() != null) {
                coinPrice.setMarketCap(dto.getTradeValue24h());
            } else {
                coinPrice.setMarketCap(BigDecimal.ZERO);
            }

            coinPrice.setLastUpdated(LocalDateTime.now());

            log.debug("코인 변환 성공: {} ({})", coinPrice.getCoinId(), coinPrice.getExchange());
            return coinPrice;
        } catch (Exception e) {
            log.error("ExchangePriceDto를 CoinPrice로 변환 실패: {}", 
                    dto != null ? dto.getSymbol() : "null", e);
            return null;
        }
    }
}
