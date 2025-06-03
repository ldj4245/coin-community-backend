package com.coincommunity.backend.external.exchange.coingecko;

import com.coincommunity.backend.entity.CoinPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CoinGecko API를 통해 코인 가격 정보를 가져오는 클라이언트
 * https://api.coingecko.com/api/v3/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinGeckoApiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;
    
    @Value("${external.coingecko.api-key:}")
    private String apiKey;
    
    private static final String EXCHANGE = "COINGECKO";
    
    /**
     * 주요 코인들의 현재 시세 정보를 조회합니다.
     */
    public List<CoinPrice> getTopCoinPrices() {
        List<String> topCoins = List.of("bitcoin", "ethereum", "binancecoin", "ripple", 
                "cardano", "solana", "dogecoin", "polkadot", "avalanche-2", "polygon");
        
        return getCoinPrices(topCoins);
    }
    
    /**
     * 특정 코인들의 가격 정보를 조회합니다.
     */
    public List<CoinPrice> getCoinPrices(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String ids = String.join(",", coinIds);
            String url = baseUrl + "/simple/price?ids=" + ids + 
                    "&vs_currencies=krw&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true";
            
            if (!apiKey.isEmpty()) {
                url += "&x_cg_demo_api_key=" + apiKey;
            }
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                return parseCoinPriceResponse(response);
            }
        } catch (Exception e) {
            log.error("CoinGecko API 호출 중 오류가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * API 응답을 CoinPrice 엔티티 목록으로 변환합니다.
     */
    private List<CoinPrice> parseCoinPriceResponse(String response) {
        List<CoinPrice> coinPrices = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            
            rootNode.fields().forEachRemaining(entry -> {
                String coinId = entry.getKey();
                JsonNode priceData = entry.getValue();
                
                try {
                    CoinPrice coinPrice = new CoinPrice();
                    coinPrice.setCoinId(mapCoinGeckoIdToSymbol(coinId));
                    coinPrice.setKoreanName(getCoinKoreanName(coinId));
                    coinPrice.setEnglishName(getCoinEnglishName(coinId));
                    
                    if (priceData.has("krw")) {
                        coinPrice.setCurrentPrice(new BigDecimal(priceData.get("krw").asText()));
                    }
                    
                    if (priceData.has("krw_24h_change")) {
                        coinPrice.setPriceChangePercent(new BigDecimal(priceData.get("krw_24h_change").asText()));
                    }
                    
                    if (priceData.has("krw_24h_vol")) {
                        coinPrice.setVolume24h(new BigDecimal(priceData.get("krw_24h_vol").asText()));
                    }
                    
                    if (priceData.has("krw_market_cap")) {
                        coinPrice.setMarketCap(new BigDecimal(priceData.get("krw_market_cap").asText()));
                    }
                    
                    coinPrice.setExchange(EXCHANGE);
                    coinPrice.setLastUpdated(LocalDateTime.now());
                    
                    coinPrices.add(coinPrice);
                } catch (Exception e) {
                    log.warn("CoinGecko 데이터 변환 중 오류: coinId={}", coinId, e);
                }
            });
        } catch (Exception e) {
            log.error("CoinGecko 응답 파싱 중 오류가 발생했습니다", e);
        }
        
        return coinPrices;
    }
    
    /**
     * CoinGecko ID를 심볼로 변환합니다.
     */
    private String mapCoinGeckoIdToSymbol(String coinGeckoId) {
        switch (coinGeckoId.toLowerCase()) {
            case "bitcoin": return "BTC";
            case "ethereum": return "ETH";
            case "binancecoin": return "BNB";
            case "ripple": return "XRP";
            case "cardano": return "ADA";
            case "solana": return "SOL";
            case "dogecoin": return "DOGE";
            case "polkadot": return "DOT";
            case "avalanche-2": return "AVAX";
            case "polygon": return "MATIC";
            default: return coinGeckoId.toUpperCase();
        }
    }
    
    /**
     * 코인의 한글 이름을 반환합니다.
     */
    private String getCoinKoreanName(String coinId) {
        switch (coinId.toLowerCase()) {
            case "bitcoin": return "비트코인";
            case "ethereum": return "이더리움";
            case "binancecoin": return "바이낸스 코인";
            case "ripple": return "리플";
            case "cardano": return "에이다";
            case "solana": return "솔라나";
            case "dogecoin": return "도지코인";
            case "polkadot": return "폴카닷";
            case "avalanche-2": return "아발란치";
            case "polygon": return "폴리곤";
            default: return coinId;
        }
    }
    
    /**
     * 코인의 영어 이름을 반환합니다.
     */
    private String getCoinEnglishName(String coinId) {
        switch (coinId.toLowerCase()) {
            case "bitcoin": return "Bitcoin";
            case "ethereum": return "Ethereum";
            case "binancecoin": return "BNB";
            case "ripple": return "XRP";
            case "cardano": return "Cardano";
            case "solana": return "Solana";
            case "dogecoin": return "Dogecoin";
            case "polkadot": return "Polkadot";
            case "avalanche-2": return "Avalanche";
            case "polygon": return "Polygon";
            default: return coinId;
        }
    }
    
    /**
     * 시가총액 기준 상위 코인 목록을 조회합니다.
     */
    public List<CoinPrice> getTopCoinsByMarketCap(int limit) {
        try {
            String url = baseUrl + "/coins/markets?vs_currency=krw&order=market_cap_desc&per_page=" + 
                    limit + "&page=1&sparkline=false";
            
            if (!apiKey.isEmpty()) {
                url += "&x_cg_demo_api_key=" + apiKey;
            }
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                return parseMarketDataResponse(response);
            }
        } catch (Exception e) {
            log.error("CoinGecko 시가총액 데이터 조회 중 오류가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 마켓 데이터 응답을 파싱합니다.
     */
    private List<CoinPrice> parseMarketDataResponse(String response) {
        List<CoinPrice> coinPrices = new ArrayList<>();
        
        try {
            JsonNode arrayNode = objectMapper.readTree(response);
            
            for (JsonNode coinNode : arrayNode) {
                try {
                    CoinPrice coinPrice = new CoinPrice();
                    
                    coinPrice.setCoinId(coinNode.get("symbol").asText().toUpperCase());
                    coinPrice.setKoreanName(coinNode.get("name").asText());
                    coinPrice.setEnglishName(coinNode.get("name").asText());
                    
                    if (coinNode.has("current_price") && !coinNode.get("current_price").isNull()) {
                        coinPrice.setCurrentPrice(new BigDecimal(coinNode.get("current_price").asText()));
                    }
                    
                    if (coinNode.has("price_change_percentage_24h") && !coinNode.get("price_change_percentage_24h").isNull()) {
                        coinPrice.setPriceChangePercent(new BigDecimal(coinNode.get("price_change_percentage_24h").asText()));
                    }
                    
                    if (coinNode.has("total_volume") && !coinNode.get("total_volume").isNull()) {
                        coinPrice.setVolume24h(new BigDecimal(coinNode.get("total_volume").asText()));
                    }
                    
                    if (coinNode.has("market_cap") && !coinNode.get("market_cap").isNull()) {
                        coinPrice.setMarketCap(new BigDecimal(coinNode.get("market_cap").asText()));
                    }
                    
                    if (coinNode.has("high_24h") && !coinNode.get("high_24h").isNull()) {
                        coinPrice.setHighPrice24h(new BigDecimal(coinNode.get("high_24h").asText()));
                    }
                    
                    if (coinNode.has("low_24h") && !coinNode.get("low_24h").isNull()) {
                        coinPrice.setLowPrice24h(new BigDecimal(coinNode.get("low_24h").asText()));
                    }
                    
                    coinPrice.setExchange(EXCHANGE);
                    coinPrice.setLastUpdated(LocalDateTime.now());
                    
                    coinPrices.add(coinPrice);
                } catch (Exception e) {
                    log.warn("CoinGecko 마켓 데이터 변환 중 오류: {}", coinNode.get("id").asText(), e);
                }
            }
        } catch (Exception e) {
            log.error("CoinGecko 마켓 데이터 파싱 중 오류가 발생했습니다", e);
        }
        
        return coinPrices;
    }
}
