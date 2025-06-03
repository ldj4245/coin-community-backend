package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.CoinPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 코인 가격 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface CoinPriceRepository extends JpaRepository<CoinPrice, String> {

    /**
     * 특정 거래소의 모든 코인 가격을 조회합니다.
     */
    List<CoinPrice> findByExchange(String exchange);

    /**
     * 특정 거래소의 특정 코인 가격을 조회합니다.
     */
    Optional<CoinPrice> findByCoinIdAndExchange(String coinId, String exchange);

    /**
     * 시가총액 기준으로 상위 코인들을 조회합니다.
     */
    @Query("SELECT c FROM CoinPrice c WHERE c.marketCap IS NOT NULL ORDER BY c.marketCap DESC")
    List<CoinPrice> findTopByMarketCap(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 가격 변화율 기준으로 상승률이 높은 코인들을 조회합니다.
     */
    @Query("SELECT c FROM CoinPrice c WHERE c.priceChangePercent IS NOT NULL ORDER BY c.priceChangePercent DESC")
    List<CoinPrice> findTopGainers(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 가격 변화율 기준으로 하락률이 높은 코인들을 조회합니다.
     */
    @Query("SELECT c FROM CoinPrice c WHERE c.priceChangePercent IS NOT NULL ORDER BY c.priceChangePercent ASC")
    List<CoinPrice> findTopLosers(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 특정 코인의 최신 가격 정보를 조회합니다.
     */
    Optional<CoinPrice> findTopByCoinIdOrderByLastUpdatedDesc(String coinId);
}
