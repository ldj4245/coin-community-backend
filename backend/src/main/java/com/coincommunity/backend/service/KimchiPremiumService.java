package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.dto.ExchangeRateTableDto;
import com.coincommunity.backend.dto.KimchiPremiumDto;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 김치프리미엄 및 거래소 시세 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KimchiPremiumService {

    private final RestTemplate restTemplate;
    private final ExchangeApiStrategyContext exchangeApiContext;

    // 알림 서비스 (선택적 주입)
    @Autowired(required = false)
    private com.coincommunity.backend.websocket.PremiumNotificationService notificationService;

    // 업비트 API 연결 상태
    private boolean upbitConnected = false;
    private LocalDateTime lastUpbitConnectionCheck = LocalDateTime.now().minusHours(1);

    @Value("${external.upbit.base-url}")
    private String upbitBaseUrl;

    @Value("${external.upbit.version}")
    private String upbitVersion;

    @Value("${external.bithumb.base-url}")
    private String bithumbBaseUrl;

    @Value("${external.coinone.base-url}")
    private String coinoneBaseUrl;

    @Value("${external.korbit.base-url}")
    private String korbitBaseUrl;

    @Value("${external.binance.base-url}")
    private String binanceBaseUrl;

    // 주요 코인 목록
    private static final Map<String, String> MAJOR_COINS = Map.of(
        "BTC", "비트코인",
        "ETH", "이더리움", 
        "XRP", "리플",
        "ADA", "에이다",
        "DOT", "폴카닷",
        "LINK", "체인링크",
        "LTC", "라이트코인",
        "BCH", "비트코인캐시"
    );

    // USD 환율 (실제로는 외환 API에서 가져와야 함)
    private static final BigDecimal USD_EXCHANGE_RATE = new BigDecimal("1350.50");

    /**
     * 김치프리미엄 정보 조회
     */
    @Cacheable(value = "kimchiPremium", key = "#symbol")
    public KimchiPremiumDto getKimchiPremium(String symbol) {
        log.info("김치프리미엄 조회 시작 - 심볼: {}", symbol);

        try {
            // 국내 거래소 가격 정보 수집
            Map<String, KimchiPremiumDto.ExchangePriceInfo> domesticPrices = getDomesticExchangePrices(symbol);

            // 해외 거래소 가격 정보 수집  
            Map<String, KimchiPremiumDto.ExchangePriceInfo> foreignPrices = getForeignExchangePrices(symbol);

            if (domesticPrices.isEmpty() || foreignPrices.isEmpty()) {
                log.warn("가격 정보가 부족합니다 - 심볼: {}", symbol);
                return null;
            }

            // 김치프리미엄 계산
            KimchiPremiumCalculation calculation = calculateKimchiPremium(domesticPrices, foreignPrices);

            KimchiPremiumDto premiumDto = KimchiPremiumDto.builder()
                .symbol(symbol)
                .koreanName(MAJOR_COINS.get(symbol))
                .domesticPrices(domesticPrices)
                .foreignPrices(foreignPrices)
                .premiumRate(calculation.premiumRate)
                .premiumAmount(calculation.premiumAmount)
                .baseExchange(calculation.baseExchange)
                .highestDomesticExchange(calculation.highestDomesticExchange)
                .lowestDomesticExchange(calculation.lowestDomesticExchange)
                .updatedAt(LocalDateTime.now())
                .build();

            // 업비트 연결 상태 확인
            upbitConnected = domesticPrices.containsKey("UPBIT");
            lastUpbitConnectionCheck = LocalDateTime.now();

            // 알림 서비스가 있으면 프리미엄 알림 전송
            if (notificationService != null) {
                try {
                    notificationService.notifyPremiumChange(premiumDto);
                } catch (Exception e) {
                    log.warn("김치프리미엄 알림 처리 중 오류 발생", e);
                }
            }

            return premiumDto;

        } catch (Exception e) {
            log.error("김치프리미엄 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return null;
        }
    }

    /**
     * 전체 김치프리미엄 리스트 조회
     */
    @Cacheable(value = "kimchiPremiumList")
    public List<KimchiPremiumDto> getAllKimchiPremiums() {
        log.info("전체 김치프리미엄 리스트 조회 시작");

        List<KimchiPremiumDto> premiums = MAJOR_COINS.keySet().stream()
            .map(this::getKimchiPremium)
            .filter(Objects::nonNull)
            .sorted((a, b) -> b.getPremiumRate().compareTo(a.getPremiumRate()))
            .collect(Collectors.toList());

        // 김치프리미엄 데이터 로그
        if (!premiums.isEmpty()) {
            KimchiPremiumDto highest = premiums.get(0);
            log.info("최고 김치프리미엄: {} ({}), 프리미엄: {}%, 금액: {}", 
                     highest.getSymbol(), highest.getKoreanName(), 
                     highest.getPremiumRate(), highest.getPremiumAmount());
        }

        return premiums;
    }

    /**
     * 거래소별 시세표 조회
     */
    @Cacheable(value = "exchangeRateTable", key = "#symbol")
    public ExchangeRateTableDto getExchangeRateTable(String symbol) {
        log.info("거래소별 시세표 조회 시작 - 심볼: {}", symbol);

        try {
            List<ExchangeRateTableDto.ExchangeRate> domesticRates = getDomesticExchangeRates(symbol);
            List<ExchangeRateTableDto.ExchangeRate> foreignRates = getForeignExchangeRates(symbol);

            if (domesticRates.isEmpty()) {
                log.warn("국내 거래소 시세 정보가 없습니다 - 심볼: {}", symbol);
                return null;
            }

            // 최고가, 최저가 계산
            BigDecimal highestPrice = domesticRates.stream()
                .map(ExchangeRateTableDto.ExchangeRate::getCurrentPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            BigDecimal lowestPrice = domesticRates.stream()
                .map(ExchangeRateTableDto.ExchangeRate::getCurrentPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            BigDecimal priceDifference = highestPrice.subtract(lowestPrice);
            BigDecimal priceDifferenceRate = lowestPrice.compareTo(BigDecimal.ZERO) > 0 ?
                priceDifference.divide(lowestPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

            return ExchangeRateTableDto.builder()
                .symbol(symbol)
                .koreanName(MAJOR_COINS.get(symbol))
                .englishName(getEnglishName(symbol))
                .usdExchangeRate(USD_EXCHANGE_RATE)
                .domesticRates(domesticRates)
                .foreignRates(foreignRates)
                .priceDifference(priceDifference)
                .priceDifferenceRate(priceDifferenceRate)
                .updatedAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("거래소별 시세표 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return null;
        }
    }

    /**
     * 국내 거래소 가격 정보 수집
     */
    private Map<String, KimchiPremiumDto.ExchangePriceInfo> getDomesticExchangePrices(String symbol) {
        Map<String, KimchiPremiumDto.ExchangePriceInfo> prices = new HashMap<>();
        String normalizedSymbol = symbol.toUpperCase().replace("KRW-", "");

        // 업비트
        try {
            KimchiPremiumDto.ExchangePriceInfo upbitPrice = getUpbitPrice(normalizedSymbol);
            if (upbitPrice != null) {
                prices.put("UPBIT", upbitPrice);
            }
        } catch (Exception e) {
            log.warn("업비트 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }

        // 빗썸
        try {
            KimchiPremiumDto.ExchangePriceInfo bithumbPrice = getBithumbPrice(normalizedSymbol);
            if (bithumbPrice != null) {
                prices.put("BITHUMB", bithumbPrice);
            }
        } catch (Exception e) {
            log.warn("빗썸 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }

        // 코인원
        try {
            KimchiPremiumDto.ExchangePriceInfo coinonePrice = getCoinonePrice(normalizedSymbol);
            if (coinonePrice != null) {
                prices.put("COINONE", coinonePrice);
            }
        } catch (Exception e) {
            log.warn("코인원 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }

        // 코빗
        try {
            KimchiPremiumDto.ExchangePriceInfo korbitPrice = getKorbitPrice(normalizedSymbol);
            if (korbitPrice != null) {
                prices.put("KORBIT", korbitPrice);
            }
        } catch (Exception e) {
            log.warn("코빗 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }

        if (prices.isEmpty()) {
            log.warn("국내 거래소 가격 정보를 가져오지 못했습니다 - 심볼: {}", normalizedSymbol);
        }

        return prices;
    }

    /**
     * 해외 거래소 가격 정보 수집
     */
    private Map<String, KimchiPremiumDto.ExchangePriceInfo> getForeignExchangePrices(String symbol) {
        Map<String, KimchiPremiumDto.ExchangePriceInfo> prices = new HashMap<>();
        String normalizedSymbol = symbol.toUpperCase().replace("USDT", "");

        // 바이낸스
        try {
            KimchiPremiumDto.ExchangePriceInfo binancePrice = getBinancePrice(normalizedSymbol);
            if (binancePrice != null) {
                prices.put("BINANCE", binancePrice);
            }
        } catch (Exception e) {
            log.warn("바이낸스 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }

        // 코인게코 (선택적)
        try {
            KimchiPremiumDto.ExchangePriceInfo coingeckoPrice = getCoinGeckoPrice(normalizedSymbol);
            if (coingeckoPrice != null) {
                prices.put("COINGECKO", coingeckoPrice);
            }
        } catch (Exception e) {
            log.warn("코인게코 가격 조회 실패 - 심볼: {}", normalizedSymbol, e);
        }
        
        if (prices.isEmpty()) {
            log.warn("해외 거래소 가격 정보를 가져오지 못했습니다 - 심볼: {}", normalizedSymbol);
        }

        return prices;
    }

    /**
     * 업비트 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getUpbitPrice(String symbol) {
        try {
            log.info("업비트 가격 조회 시도 - 심볼: {}", symbol);
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("UPBIT", symbol).orElse(null);

            if (priceDto != null) {
                log.info("업비트 가격 조회 성공 - 심볼: {}, 가격: {}", symbol, priceDto.getCurrentPrice());
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }

            log.warn("업비트 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);

        } catch (Exception e) {
            log.error("업비트 가격 조회 처리 중 예외 발생 - 심볼: {}", symbol, e);
        }
        return null; // 실패 시 null 반환
    }

    /**
     * 빗썸 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getBithumbPrice(String symbol) {
        try {
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("BITHUMB", symbol).orElse(null);

            if (priceDto != null) {
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }
            log.warn("빗썸 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);
        } catch (Exception e) {
            log.error("빗썸 가격 조회 오류 - 심볼: {}", symbol, e);
        }
        return null;
    }

    /**
     * 코인원 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getCoinonePrice(String symbol) {
        try {
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("COINONE", symbol).orElse(null);

            if (priceDto != null) {
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }
            log.warn("코인원 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);
        } catch (Exception e) {
            log.error("코인원 가격 조회 오류 - 심볼: {}", symbol, e);
        }
        return null;
    }

    /**
     * 코빗 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getKorbitPrice(String symbol) {
        try {
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("KORBIT", symbol).orElse(null);

            if (priceDto != null) {
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }
            log.warn("코빗 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);
        } catch (Exception e) {
            log.error("코빗 가격 조회 오류 - 심볼: {}", symbol, e);
        }
        return null;
    }

    /**
     * 바이낸스 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getBinancePrice(String symbol) {
        try {
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("BINANCE", symbol).orElse(null);

            if (priceDto != null) {
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }
            log.warn("바이낸스 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);
        } catch (Exception e) {
            log.error("바이낸스 가격 조회 오류 - 심볼: {}", symbol, e);
        }
        return null;
    }

    /**
     * 코인게코 가격 조회
     */
    private KimchiPremiumDto.ExchangePriceInfo getCoinGeckoPrice(String symbol) {
        try {
            ExchangePriceDto priceDto = exchangeApiContext.getCoinPrice("COINGECKO", symbol).orElse(null);

            if (priceDto != null) {
                return KimchiPremiumDto.ExchangePriceInfo.from(priceDto);
            }
            log.warn("코인게코 API에서 가격 정보를 가져오지 못했습니다 - 심볼: {}", symbol);
        } catch (Exception e) {
            log.error("코인게코 가격 조회 오류 - 심볼: {}", symbol, e);
        }
        return null;
    }

    /**
     * 김치프리미엄 계산
     */
    private KimchiPremiumCalculation calculateKimchiPremium(
        Map<String, KimchiPremiumDto.ExchangePriceInfo> domesticPrices,
        Map<String, KimchiPremiumDto.ExchangePriceInfo> foreignPrices) {

        // 국내 최고가 찾기
        Map.Entry<String, KimchiPremiumDto.ExchangePriceInfo> highestDomestic = domesticPrices.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> a.getPriceKrw().compareTo(b.getPriceKrw())))
            .orElse(null);

        // 국내 최저가 찾기
        Map.Entry<String, KimchiPremiumDto.ExchangePriceInfo> lowestDomestic = domesticPrices.entrySet().stream()
            .min(Map.Entry.comparingByValue((a, b) -> a.getPriceKrw().compareTo(b.getPriceKrw())))
            .orElse(null);

        // 해외 기준가 (바이낸스)
        KimchiPremiumDto.ExchangePriceInfo binancePrice = foreignPrices.get("BINANCE");

        if (highestDomestic == null || binancePrice == null) {
            return new KimchiPremiumCalculation(BigDecimal.ZERO, BigDecimal.ZERO, "", "", "");
        }

        BigDecimal domesticPrice = highestDomestic.getValue().getPriceKrw();
        BigDecimal foreignPriceKrw = binancePrice.getPriceUsd().multiply(USD_EXCHANGE_RATE);

        BigDecimal premiumAmount = domesticPrice.subtract(foreignPriceKrw);
        BigDecimal premiumRate = foreignPriceKrw.compareTo(BigDecimal.ZERO) > 0 ?
            premiumAmount.divide(foreignPriceKrw, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;

        return new KimchiPremiumCalculation(
            premiumRate,
            premiumAmount,
            "BINANCE",
            highestDomestic.getKey(),
            lowestDomestic != null ? lowestDomestic.getKey() : ""
        );
    }

    /**
     * 국내 거래소 시세 정보 수집
     */
    private List<ExchangeRateTableDto.ExchangeRate> getDomesticExchangeRates(String symbol) {
        List<ExchangeRateTableDto.ExchangeRate> rates = new ArrayList<>();

        try {
            // 국내 거래소 API를 통해 실제 데이터 조회
            List<ExchangePriceDto> domesticPrices = exchangeApiContext.getDomesticExchangePrices(symbol);

            for (ExchangePriceDto priceDto : domesticPrices) {
                ExchangeRateTableDto.ExchangeRate rate = ExchangeRateTableDto.ExchangeRate.builder()
                    .exchangeName(priceDto.getExchangeName())
                    .exchangeKoreanName(priceDto.getExchangeKoreanName())
                    .currentPrice(priceDto.getCurrentPrice())
                    .changePrice(new BigDecimal("1000000")) // 임의 설정
                    .changeRate(priceDto.getChangeRate())
                    .highPrice24h(priceDto.getHighPrice24h())
                    .lowPrice24h(priceDto.getLowPrice24h())
                    .volume24h(priceDto.getVolume24h())
                    .tradeValue24h(priceDto.getVolume24h().multiply(priceDto.getCurrentPrice()))
                    .bidPrice(priceDto.getCurrentPrice().subtract(new BigDecimal("50000")))
                    .askPrice(priceDto.getCurrentPrice().add(new BigDecimal("50000")))
                    .spread(new BigDecimal("100000"))
                    .spreadRate(new BigDecimal("0.22"))
                    .exchangeType(ExchangeRateTableDto.ExchangeType.DOMESTIC)
                    .status(ExchangeRateTableDto.TradingStatus.valueOf(priceDto.getStatus().name()))
                    .lastUpdated(priceDto.getLastUpdated())
                    .build();

                rates.add(rate);
            }
        } catch (Exception e) {
            log.error("국내 거래소 시세 정보 수집 오류 - 심볼: {}", symbol, e);
        }

        return rates;
    }

    /**
     * 해외 거래소 시세 정보 수집
     */
    private List<ExchangeRateTableDto.ExchangeRate> getForeignExchangeRates(String symbol) {
        List<ExchangeRateTableDto.ExchangeRate> rates = new ArrayList<>();

        try {
            // 해외 거래소 API를 통해 실제 데이터 조회
            List<ExchangePriceDto> foreignPrices = exchangeApiContext.getForeignExchangePrices(symbol);

            for (ExchangePriceDto priceDto : foreignPrices) {
                ExchangeRateTableDto.ExchangeRate rate = ExchangeRateTableDto.ExchangeRate.builder()
                    .exchangeName(priceDto.getExchangeName())
                    .exchangeKoreanName(priceDto.getExchangeKoreanName())
                    .currentPrice(priceDto.getCurrentPrice())
                    .changePrice(new BigDecimal("1000000")) // 임의 설정
                    .changeRate(priceDto.getChangeRate())
                    .highPrice24h(priceDto.getHighPrice24h())
                    .lowPrice24h(priceDto.getLowPrice24h())
                    .volume24h(priceDto.getVolume24h())
                    .tradeValue24h(priceDto.getVolume24h().multiply(priceDto.getCurrentPrice()))
                    .bidPrice(priceDto.getCurrentPrice().subtract(new BigDecimal("50000")))
                    .askPrice(priceDto.getCurrentPrice().add(new BigDecimal("50000")))
                    .spread(new BigDecimal("100000"))
                    .spreadRate(new BigDecimal("0.22"))
                    .exchangeType(ExchangeRateTableDto.ExchangeType.FOREIGN)
                    .status(ExchangeRateTableDto.TradingStatus.valueOf(priceDto.getStatus().name()))
                    .lastUpdated(priceDto.getLastUpdated())
                    .build();

                rates.add(rate);
            }
        } catch (Exception e) {
            log.error("해외 거래소 시세 정보 수집 오류 - 심볼: {}", symbol, e);
        }

        return rates;
    }

    /**
     * 모의 거래소 시세 데이터 생성
     */
    private ExchangeRateTableDto.ExchangeRate createMockExchangeRate(String exchangeName, String koreanName, BigDecimal currentPrice, ExchangeRateTableDto.ExchangeType type) {
        BigDecimal changePrice = new BigDecimal("1000000");
        BigDecimal changeRate = changePrice.divide(currentPrice.subtract(changePrice), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        return ExchangeRateTableDto.ExchangeRate.builder()
            .exchangeName(exchangeName)
            .exchangeKoreanName(koreanName)
            .currentPrice(currentPrice)
            .changePrice(changePrice)
            .changeRate(changeRate)
            .highPrice24h(currentPrice.multiply(new BigDecimal("1.05")))
            .lowPrice24h(currentPrice.multiply(new BigDecimal("0.95")))
            .volume24h(new BigDecimal("123.45"))
            .tradeValue24h(currentPrice.multiply(new BigDecimal("123.45")))
            .bidPrice(currentPrice.subtract(new BigDecimal("50000")))
            .askPrice(currentPrice.add(new BigDecimal("50000")))
            .spread(new BigDecimal("100000"))
            .spreadRate(new BigDecimal("0.22"))
            .exchangeType(type)
            .status(ExchangeRateTableDto.TradingStatus.NORMAL)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    /**
     * 영문명 조회
     */
    private String getEnglishName(String symbol) {
        Map<String, String> englishNames = Map.of(
            "BTC", "Bitcoin",
            "ETH", "Ethereum",
            "XRP", "Ripple",
            "ADA", "Cardano",
            "DOT", "Polkadot",
            "LINK", "Chainlink",
            "LTC", "Litecoin",
            "BCH", "Bitcoin Cash"
        );
        return englishNames.getOrDefault(symbol, symbol);
    }

    /**
     * 김치프리미엄 계산 결과 클래스
     */
    private static class KimchiPremiumCalculation {
        final BigDecimal premiumRate;
        final BigDecimal premiumAmount;
        final String baseExchange;
        final String highestDomesticExchange;
        final String lowestDomesticExchange;

        KimchiPremiumCalculation(BigDecimal premiumRate, BigDecimal premiumAmount, String baseExchange, 
                               String highestDomesticExchange, String lowestDomesticExchange) {
            this.premiumRate = premiumRate;
            this.premiumAmount = premiumAmount;
            this.baseExchange = baseExchange;
            this.highestDomesticExchange = highestDomesticExchange;
            this.lowestDomesticExchange = lowestDomesticExchange;
        }
    }

    /**
     * 애플리케이션 시작 시 실행되는 초기 데이터 로드 메소드
     * 캐시 데이터를 미리 구성하기 위함
     */
    public void loadInitialData() {
        log.info("김치프리미엄 초기 데이터 로드 시작");
        try {
            // 업비트 API 건강 상태 먼저 확인
            boolean upbitHealthy = checkUpbitHealth();
            log.info("업비트 API 상태 확인 결과: {}", upbitHealthy ? "정상" : "연결 실패");

            // 주요 코인 정보 로드
            for (String symbol : MAJOR_COINS.keySet()) {
                try {
                    KimchiPremiumDto premium = getKimchiPremium(symbol);
                    if (premium != null) {
                        boolean hasUpbit = premium.getDomesticPrices().containsKey("UPBIT");
                        log.info("{}({}) 김치프리미엄 정보 로드 완료 - 업비트 데이터 {}", 
                                MAJOR_COINS.get(symbol), symbol, hasUpbit ? "포함" : "미포함");
                    } else {
                        log.warn("{}({}) 김치프리미엄 정보 로드 실패 - 데이터 없음", MAJOR_COINS.get(symbol), symbol);
                    }
                } catch (Exception e) {
                    log.error("{}({}) 김치프리미엄 정보 로드 실패", MAJOR_COINS.get(symbol), symbol, e);
                }
            }
            log.info("김치프리미엄 초기 데이터 로드 완료");
        } catch (Exception e) {
            log.error("김치프리미엄 초기 데이터 로드 중 오류 발생", e);
        }
    }

    /**
     * 업비트 API 건강 상태 체크
     */
    private boolean checkUpbitHealth() {
        try {
            String url = upbitBaseUrl + "/" + upbitVersion + "/market/all";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean isSuccess = response.getStatusCode().is2xxSuccessful() && 
                                response.getBody() != null && 
                                !response.getBody().isEmpty();

            if (isSuccess) {
                log.info("업비트 API 연결 성공");
                return true;
            } else {
                log.warn("업비트 API 응답은 받았으나 데이터가 없습니다. 상태코드: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("업비트 API 건강 상태 확인 실패", e);
            return false;
        }
    }
}
