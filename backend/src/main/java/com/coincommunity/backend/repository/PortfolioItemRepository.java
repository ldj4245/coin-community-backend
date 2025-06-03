package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 아이템 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 성능 최적화된 쿼리
 * - 인덱스 활용 고려
 * - 데이터 정합성 보장
 */
@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    /**
     * 포트폴리오별 아이템 조회 (수량 내림차순)
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId ORDER BY pi.currentValue DESC")
    List<PortfolioItem> findByPortfolioIdOrderByCurrentValueDesc(@Param("portfolioId") Long portfolioId);

    /**
     * 특정 포트폴리오의 특정 코인 아이템 조회
     */
    Optional<PortfolioItem> findByPortfolioIdAndCoinId(Long portfolioId, String coinId);

    /**
     * 사용자의 모든 코인 보유 현황 조회 (포트폴리오 통합)
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.user.id = :userId")
    List<PortfolioItem> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 코인을 보유한 모든 아이템 조회 (실시간 가격 업데이트용)
     */
    List<PortfolioItem> findByCoinId(String coinId);

    /**
     * 수량이 0보다 큰 아이템만 조회 (실제 보유 중인 코인)
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.quantity > 0")
    List<PortfolioItem> findActiveItemsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오의 총 아이템 수 조회
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * 특정 사용자의 보유 코인 종류 수 조회
     */
    @Query("SELECT COUNT(DISTINCT pi.coinId) FROM PortfolioItem pi WHERE pi.portfolio.user.id = :userId AND pi.quantity > 0")
    long countDistinctCoinsByUserId(@Param("userId") Long userId);

    /**
     * 상위 수익률 아이템 조회 (랭킹용)
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.quantity > 0 ORDER BY pi.unrealizedGainPercent DESC")
    List<PortfolioItem> findTopPerformingItems(org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 수익률 이상의 아이템 조회
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.unrealizedGainPercent >= :minGainPercent")
    List<PortfolioItem> findProfitableItems(@Param("portfolioId") Long portfolioId, @Param("minGainPercent") BigDecimal minGainPercent);

    /**
     * 손실 중인 아이템 조회
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.unrealizedGainPercent < 0")
    List<PortfolioItem> findLosingItems(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오의 평균 수익률 계산
     */
    @Query("SELECT AVG(pi.unrealizedGainPercent) FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.quantity > 0")
    Optional<BigDecimal> findAverageGainPercentByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 최근 업데이트된 아이템 조회 (가격 업데이트 확인용)
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.lastUpdatedAt > :since ORDER BY pi.lastUpdatedAt DESC")
    List<PortfolioItem> findRecentlyUpdatedItems(@Param("since") java.time.LocalDateTime since);

    /**
     * 특정 기간 내 최초 매수한 아이템 조회
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.firstPurchaseDate BETWEEN :startDate AND :endDate")
    List<PortfolioItem> findByFirstPurchaseDateBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * 가장 오래된 보유 아이템 조회
     */
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.user.id = :userId AND pi.quantity > 0 ORDER BY pi.firstPurchaseDate ASC")
    List<PortfolioItem> findOldestHoldingsByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 포트폴리오 가치 계산을 위한 집계 쿼리
     */
    @Query("SELECT " +
           "SUM(pi.totalInvestment) as totalInvestment, " +
           "SUM(pi.currentValue) as currentValue, " +
           "SUM(pi.unrealizedGain) as totalUnrealizedGain " +
           "FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.quantity > 0")
    Object[] calculatePortfolioSummary(@Param("portfolioId") Long portfolioId);

    /**
     * 수량이 0인 아이템 정리 (배치 작업용)
     */
    @Query("DELETE FROM PortfolioItem pi WHERE pi.quantity = 0")
    void deleteZeroQuantityItems();
}
