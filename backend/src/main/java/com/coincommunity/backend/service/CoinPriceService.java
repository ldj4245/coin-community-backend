package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.CoinPriceDto;
import com.coincommunity.backend.entity.CoinPrice;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.CoinPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 코인 가격 정보 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinPriceService {
    
    private final CoinPriceRepository coinPriceRepository;
    
    /**
     * 코인 ID로 코인 가격 정보를 조회합니다.
     */
    public CoinPrice findById(String id) {
        return coinPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("코인 가격 정보를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 특정 거래소의 특정 코인 가격 정보를 조회합니다.
     */
    @Cacheable(value = "coinPrices", key = "#coinId + '-' + #exchange")
    public CoinPriceDto.CoinPriceResponse getCoinPriceByExchange(String coinId, String exchange) {
        CoinPrice coinPrice = coinPriceRepository.findByCoinIdAndExchange(coinId, exchange)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "해당 거래소의 코인 가격 정보를 찾을 수 없습니다. 코인: " + coinId + ", 거래소: " + exchange));
        return CoinPriceDto.CoinPriceResponse.from(coinPrice);
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
     */
    @Transactional
    @CacheEvict(value = {"coinPrices", "allCoinPrices", "exchangeCoinPrices", "topMarketCapCoins", "topGainers", "topLosers"}, allEntries = true)
    public List<CoinPrice> saveAllCoinPrices(List<CoinPrice> coinPrices) {
        return coinPriceRepository.saveAll(coinPrices);
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
