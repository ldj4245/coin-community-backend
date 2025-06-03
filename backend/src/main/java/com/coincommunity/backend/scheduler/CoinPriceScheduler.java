package com.coincommunity.backend.scheduler;

import com.coincommunity.backend.entity.CoinPrice;
import com.coincommunity.backend.external.exchange.upbit.UpbitApiClient;
import com.coincommunity.backend.external.exchange.coingecko.CoinGeckoApiClient;
import com.coincommunity.backend.service.CoinPriceService;
import com.coincommunity.backend.websocket.CoinPriceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 코인 가격 정보를 주기적으로 업데이트하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinPriceScheduler {
    
    private final UpbitApiClient upbitApiClient;
    private final CoinGeckoApiClient coinGeckoApiClient;
    private final CoinPriceService coinPriceService;
    private final CoinPriceWebSocketHandler coinPriceWebSocketHandler;
    
    @Value("${scheduler.coin-price.enable-upbit:true}")
    private boolean enableUpbit;
    
    @Value("${scheduler.coin-price.enable-coingecko:true}")
    private boolean enableCoinGecko;
    
    /**
     * 1분마다 주요 코인 가격 정보를 업데이트합니다.
     */
    @Scheduled(fixedRate = 60000)
    public void updateCoinPrices() {
        try {
            log.debug("코인 가격 정보 업데이트 시작");
            List<CoinPrice> allCoinPrices = new ArrayList<>();
            
            // Upbit API에서 데이터 수집
            if (enableUpbit) {
                try {
                    List<CoinPrice> upbitPrices = upbitApiClient.getTopCoinPrices();
                    allCoinPrices.addAll(upbitPrices);
                    log.debug("Upbit에서 {}개 코인 가격 정보 수집", upbitPrices.size());
                } catch (Exception e) {
                    log.error("Upbit API 호출 중 오류 발생", e);
                }
            }
            
            // CoinGecko API에서 데이터 수집
            if (enableCoinGecko) {
                try {
                    List<CoinPrice> coinGeckoPrices = coinGeckoApiClient.getTopCoinsByMarketCap(50);
                    allCoinPrices.addAll(coinGeckoPrices);
                    log.debug("CoinGecko에서 {}개 코인 가격 정보 수집", coinGeckoPrices.size());
                } catch (Exception e) {
                    log.error("CoinGecko API 호출 중 오류 발생", e);
                }
            }
            
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
        if (!enableCoinGecko) {
            return;
        }
        
        try {
            log.debug("시가총액 상위 코인 정보 업데이트 시작");
            List<CoinPrice> topCoins = coinGeckoApiClient.getTopCoinsByMarketCap(100);
            
            if (!topCoins.isEmpty()) {
                coinPriceService.saveAllCoinPrices(topCoins);
                log.debug("시가총액 상위 코인 정보 업데이트 완료: {}개 코인", topCoins.size());
            }
        } catch (Exception e) {
            log.error("시가총액 상위 코인 정보 업데이트 중 오류 발생", e);
        }
    }
}
