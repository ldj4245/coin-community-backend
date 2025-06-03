package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.CoinPriceDto;
import com.coincommunity.backend.entity.CoinPrice;
import com.coincommunity.backend.entity.CoinPriceId;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.CoinPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 코인 가격 정보 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinPriceService {
    
    private final CoinPriceRepository coinPriceRepository;
    
    /**
     * 코인 ID와 거래소로 코인 가격 정보를 조회합니다.
     */
    public CoinPrice findById(String coinId, String exchange) {
        return coinPriceRepository.findById(new CoinPriceId(coinId, exchange))
                .orElseThrow(() -> new ResourceNotFoundException("코인 가격 정보를 찾을 수 없습니다. 코인: " + coinId + ", 거래소: " + exchange));
    }

    /**
     * 코인 ID로 가장 최신 가격 정보를 조회합니다.
     * @deprecated 거래소 정보 없이는 특정 코인을 찾을 수 없습니다. findById(coinId, exchange)를 사용하세요.
     */
    @Deprecated
    public CoinPrice findById(String id) {
        return coinPriceRepository.findTopByCoinIdOrderByLastUpdatedDesc(id)
                .orElseThrow(() -> new ResourceNotFoundException("코인 가격 정보를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 특정 거래소의 특정 코인 가격 정보를 조회합니다.
     * 하이브리드 전략: 주요 코인이 아니면서 요청한 거래소가 COINGECKO가 아닌 경우 실시간 API 호출
     */
    @Cacheable(value = "coinPrices", key = "#coinId + '-' + #exchange")
    public CoinPriceDto.CoinPriceResponse getCoinPriceByExchange(String coinId, String exchange) {
        try {
            // 1. DB에서 조회 시도
            return coinPriceRepository.findByCoinIdAndExchange(coinId, exchange)
                    .map(CoinPriceDto.CoinPriceResponse::from)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "해당 거래소의 코인 가격 정보를 찾을 수 없습니다. 코인: " + coinId + ", 거래소: " + exchange));
        } catch (ResourceNotFoundException e) {
            // DB에 데이터가 없는 경우, COINGECKO 데이터 조회
            return coinPriceRepository.findByCoinIdAndExchange(coinId, "COINGECKO")
                    .map(coinPrice -> {
                        // COINGECKO 데이터를 바탕으로 응답 생성 (거래소 정보만 변경)
                        CoinPriceDto.CoinPriceResponse response = CoinPriceDto.CoinPriceResponse.from(coinPrice);
                        response.setExchange(exchange);
                        response.setNote("* COINGECKO 데이터 기반 추정치");
                        return response;
                    })
                    .orElseThrow(() -> e); // 그래도 없으면 원래 예외 발생
        }
    }
    
    /**
     * 모든 코인 가격 정보를 조회합니다.
     */
    @Cacheable(value = "allCoinPrices")
    public List<CoinPriceDto.CoinPriceResponse> getAllCoinPrices() {
        List<CoinPrice> coinPrices = coinPriceRepository.findAll();
        return CoinPriceDto.CoinPriceResponse.fromList(coinPrices);
    }
    
    /**
     * 특정 거래소의 모든 코인 가격 정보를 조회합니다.
     */
    @Cacheable(value = "exchangeCoinPrices", key = "#exchange")
    public List<CoinPriceDto.CoinPriceResponse> getCoinPricesByExchange(String exchange) {
        List<CoinPrice> coinPrices = coinPriceRepository.findByExchange(exchange);
        return CoinPriceDto.CoinPriceResponse.fromList(coinPrices);
    }
    
    /**
     * 시가총액 기준 상위 코인 목록을 조회합니다.
     */
    @Cacheable(value = "topMarketCapCoins", key = "#limit")
    public List<CoinPriceDto.CoinPriceResponse> getTopCoinsByMarketCap(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<CoinPrice> topCoins = coinPriceRepository.findTopByMarketCap(pageable);
        return CoinPriceDto.CoinPriceResponse.fromList(topCoins);
    }
    
    /**
     * 상승률 기준 상위 코인 목록을 조회합니다.
     */
    @Cacheable(value = "topGainers", key = "#limit")
    public List<CoinPriceDto.CoinPriceResponse> getTopGainers(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<CoinPrice> topGainers = coinPriceRepository.findTopGainers(pageable);
        return CoinPriceDto.CoinPriceResponse.fromList(topGainers);
    }
    
    /**
     * 하락률 기준 상위 코인 목록을 조회합니다.
     */
    @Cacheable(value = "topLosers", key = "#limit")
    public List<CoinPriceDto.CoinPriceResponse> getTopLosers(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<CoinPrice> topLosers = coinPriceRepository.findTopLosers(pageable);
        return CoinPriceDto.CoinPriceResponse.fromList(topLosers);
    }
    
    /**
     * 코인 가격 정보를 저장하거나 업데이트합니다.
     */
    @Transactional
    @CacheEvict(value = {"coinPrices", "allCoinPrices", "exchangeCoinPrices", "topMarketCapCoins", "topGainers", "topLosers"}, allEntries = true)
    public CoinPrice saveCoinPrice(CoinPrice coinPrice) {
        return coinPriceRepository.save(coinPrice);
    }
    
    /**
     * 여러 코인 가격 정보를 일괄 저장하거나 업데이트합니다.
     * 중복 키 충돌 방지를 위해 이미 존재하는 엔티티는 업데이트합니다.
     */
    @Transactional
    @CacheEvict(value = {"coinPrices", "allCoinPrices", "exchangeCoinPrices", "topMarketCapCoins", "topGainers", "topLosers"}, allEntries = true)
    public List<CoinPrice> saveAllCoinPrices(List<CoinPrice> coinPrices) {
        List<CoinPrice> result = new ArrayList<>();

        for (CoinPrice coinPrice : coinPrices) {
            try {
                // 이미 존재하는 엔티티인지 확인
                String coinId = coinPrice.getCoinId();
                String exchange = coinPrice.getExchange();

                boolean exists = coinPriceRepository.existsById(new CoinPriceId(coinId, exchange));

                if (exists) {
                    // 이미 존재하는 경우: 기존 엔티티 조회 후 업데이트
                    CoinPrice existingCoinPrice = coinPriceRepository.findById(new CoinPriceId(coinId, exchange))
                            .orElseThrow(); // 존재한다고 확인했으므로 결과가 없을 수 없음

                    // 기존 엔티티 업데이트
                    existingCoinPrice.setCurrentPrice(coinPrice.getCurrentPrice());
                    existingCoinPrice.setPriceChangePercent(coinPrice.getPriceChangePercent());
                    existingCoinPrice.setVolume24h(coinPrice.getVolume24h());
                    existingCoinPrice.setMarketCap(coinPrice.getMarketCap());
                    existingCoinPrice.setHighPrice24h(coinPrice.getHighPrice24h());
                    existingCoinPrice.setLowPrice24h(coinPrice.getLowPrice24h());
                    existingCoinPrice.setLastUpdated(coinPrice.getLastUpdated());

                    // 이름 정보가 비어있지 않은 경우에만 업데이트
                    if (coinPrice.getKoreanName() != null && !coinPrice.getKoreanName().isEmpty()) {
                        existingCoinPrice.setKoreanName(coinPrice.getKoreanName());
                    }
                    if (coinPrice.getEnglishName() != null && !coinPrice.getEnglishName().isEmpty()) {
                        existingCoinPrice.setEnglishName(coinPrice.getEnglishName());
                    }

                    result.add(coinPriceRepository.save(existingCoinPrice));
                } else {
                    // 새로운 엔티티인 경우: 그대로 저장
                    result.add(coinPriceRepository.save(coinPrice));
                }
            } catch (Exception e) {
                // 특정 코인 처리 중 오류가 발생해도 다른 코인은 계속 처리
                log.error("코인 가격 정보 저장 중 오류 발생. 코인: {}, 거래소: {}",
                        coinPrice.getCoinId(), coinPrice.getExchange(), e);
            }
        }

        return result;
    }
    
    /**
     * WebSocket으로 전송할 실시간 코인 가격 업데이트 정보를 생성합니다.
     */
    public CoinPriceDto.RealtimeUpdate createRealtimeUpdate(CoinPrice coinPrice) {
        return CoinPriceDto.RealtimeUpdate.from(coinPrice);
    }

    /**
     * 코인의 현재 가격을 조회합니다.
     */
    @Cacheable(value = "currentPrice", key = "#coinId")
    public BigDecimal getCurrentPrice(String coinId) {
        CoinPrice coinPrice = coinPriceRepository.findTopByCoinIdOrderByLastUpdatedDesc(coinId)
                .orElseThrow(() -> new ResourceNotFoundException("코인 가격 정보를 찾을 수 없습니다. 코인 ID: " + coinId));
        return coinPrice.getCurrentPrice();
    }
}
