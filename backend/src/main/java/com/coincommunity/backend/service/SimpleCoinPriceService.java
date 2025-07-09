package com.coincommunity.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 간단한 코인 가격 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleCoinPriceService {
    
    private final WebClient webClient = WebClient.builder().build();
    
    @Value("${external.coingecko.base-url}")
    private String coingeckoBaseUrl;
    
    /**
     * 코인 가격 조회 (CoinGecko API 사용)
     */
    public BigDecimal getCoinPrice(String coinId) {
        try {
            String url = coingeckoBaseUrl + "/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";
            
            Map<String, Map<String, Double>> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.containsKey(coinId)) {
                Double price = response.get(coinId).get("usd");
                return BigDecimal.valueOf(price);
            }
            
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to fetch coin price for {}: {}", coinId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 주요 코인들의 가격 조회
     */
    public Map<String, BigDecimal> getMajorCoinPrices() {
        try {
            String url = coingeckoBaseUrl + "/api/v3/simple/price?ids=bitcoin,ethereum,binancecoin,cardano,solana&vs_currencies=usd";
            
            Map<String, Map<String, Double>> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            return response.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> BigDecimal.valueOf(entry.getValue().get("usd"))
                    ));
        } catch (Exception e) {
            log.error("Failed to fetch major coin prices: {}", e.getMessage());
            return Map.of();
        }
    }
}