package com.coincommunity.backend.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 주요 코인 관리 서비스
 * 하이브리드 전략에서 주요 코인과 기타 코인을 구분하는 데 사용됩니다.
 */
@Service
public class MajorCoinService {

    // 주요 코인 목록 - 모든 거래소 데이터를 저장할 코인들
    @Getter
    private final Set<String> majorCoins = new HashSet<>();

    // 거래소 목록
    @Getter
    private final Set<String> supportedExchanges = new HashSet<>();

    public MajorCoinService() {
        initMajorCoins();
        initSupportedExchanges();
    }

    /**
     * 주요 코인 목록 초기화
     */
    private void initMajorCoins() {
        // 시가총액 상위 코인들
        majorCoins.add("BTC");
        majorCoins.add("ETH");
        majorCoins.add("XRP");
        majorCoins.add("USDT");
        majorCoins.add("SOL");
        majorCoins.add("ADA");
        majorCoins.add("DOGE");
        majorCoins.add("DOT");
        majorCoins.add("LINK");
        majorCoins.add("AVAX");
        majorCoins.add("LTC");
        majorCoins.add("BCH");
        majorCoins.add("MATIC");
        majorCoins.add("UNI");
        majorCoins.add("SHIB");
        majorCoins.add("XLM");
        majorCoins.add("ATOM");
        majorCoins.add("NEAR");
        majorCoins.add("ALGO");
        majorCoins.add("XTZ");
    }

    /**
     * 지원하는 거래소 목록 초기화
     */
    private void initSupportedExchanges() {
        // 국내 거래소
        supportedExchanges.add("UPBIT");
        supportedExchanges.add("BITHUMB");
        supportedExchanges.add("COINONE");
        supportedExchanges.add("KORBIT");

        // 해외 거래소
        supportedExchanges.add("BINANCE");

        // 코인게코 (기타 코인 데이터 소스)
        supportedExchanges.add("COINGECKO");
    }

    /**
     * 주요 코인인지 확인
     */
    public boolean isMajorCoin(String coinId) {
        return majorCoins.contains(coinId.toUpperCase());
    }

    /**
     * 지원하는 거래소인지 확인
     */
    public boolean isSupportedExchange(String exchange) {
        return supportedExchanges.contains(exchange.toUpperCase());
    }
}
