package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.CoinWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 코인 관심종목 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 사용자 맞춤형 알림 최적화
 * - 실시간 가격 모니터링 지원
 * - 카테고리별 효율적 조회
 */
@Repository
public interface CoinWatchlistRepository extends JpaRepository<CoinWatchlist, Long> {

    /**
     * 사용자의 관심종목 목록 조회 (추가일 최신순)
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId ORDER BY cw.createdAt DESC")
    List<CoinWatchlist> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자의 특정 코인 관심종목 조회
     */
    Optional<CoinWatchlist> findByUserIdAndCoinId(Long userId, String coinId);

    /**
     * 카테고리별 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId AND cw.category = :category ORDER BY cw.createdAt DESC")
    List<CoinWatchlist> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") CoinWatchlist.WatchlistCategory category);

    /**
     * 알림이 활성화된 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId AND cw.alertEnabled = true")
    List<CoinWatchlist> findByUserIdAndIsAlertEnabledTrue(@Param("userId") Long userId);

    /**
     * 특정 가격 알림 조건에 해당하는 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.alertEnabled = true AND " +
           "((cw.targetHighPrice IS NOT NULL AND :currentPrice >= cw.targetHighPrice) OR " +
           "(cw.targetLowPrice IS NOT NULL AND :currentPrice <= cw.targetLowPrice))")
    List<CoinWatchlist> findTriggeredPriceAlerts(@Param("currentPrice") BigDecimal currentPrice);

    /**
     * 특정 코인의 모든 관심종목 조회 (가격 업데이트 알림용)
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.coinId = :coinId AND cw.alertEnabled = true")
    List<CoinWatchlist> findByCoinIdAndIsAlertEnabledTrue(@Param("coinId") String coinId);

    /**
     * 사용자의 관심종목 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 카테고리별 관심종목 수 조회
     */
    @Query("SELECT cw.category, COUNT(cw) FROM CoinWatchlist cw WHERE cw.user.id = :userId GROUP BY cw.category")
    List<Object[]> countByUserIdGroupByCategory(@Param("userId") Long userId);

    /**
     * 가장 많이 관심종목으로 등록된 코인 조회 (인기 코인 분석)
     */
    @Query("SELECT cw.coinId, cw.coinName, COUNT(cw) as watchCount " +
           "FROM CoinWatchlist cw GROUP BY cw.coinId, cw.coinName ORDER BY watchCount DESC")
    List<Object[]> findMostWatchedCoins(org.springframework.data.domain.Pageable pageable);

    /**
     * 최근 추가된 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId ORDER BY cw.createdAt DESC")
    List<CoinWatchlist> findRecentWatchlistByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 중복 등록 체크
     */
    boolean existsByUserIdAndCoinId(Long userId, String coinId);

    /**
     * 알림 설정 일괄 업데이트를 위한 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId AND cw.coinId IN :coinIds")
    List<CoinWatchlist> findByUserIdAndCoinIdIn(@Param("userId") Long userId, @Param("coinIds") List<String> coinIds);

    /**
     * 사용자의 관심종목 통계
     */
    @Query("SELECT " +
           "COUNT(cw) as totalCount, " +
           "SUM(CASE WHEN cw.alertEnabled = true THEN 1 ELSE 0 END) as alertEnabledCount, " +
           "COUNT(DISTINCT cw.category) as categoryCount " +
           "FROM CoinWatchlist cw WHERE cw.user.id = :userId")
    Object[] findWatchlistStatsByUserId(@Param("userId") Long userId);

    /**
     * 특정 기간 내 추가된 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.createdAt BETWEEN :startDate AND :endDate")
    List<CoinWatchlist> findByCreatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * 메모가 있는 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId AND cw.memo IS NOT NULL AND cw.memo != ''")
    List<CoinWatchlist> findByUserIdWithMemo(@Param("userId") Long userId);

    /**
     * 가격 알림이 설정된 관심종목 조회
     */
    @Query("SELECT cw FROM CoinWatchlist cw WHERE cw.user.id = :userId AND " +
           "(cw.targetHighPrice IS NOT NULL OR cw.targetLowPrice IS NOT NULL)")
    List<CoinWatchlist> findByUserIdWithPriceAlert(@Param("userId") Long userId);

    /**
     * 코인별 평균 목표가 조회 (시장 분석용)
     */
    @Query("SELECT cw.coinId, AVG(cw.targetHighPrice) as avgTargetHigh, AVG(cw.targetLowPrice) as avgTargetLow " +
           "FROM CoinWatchlist cw WHERE cw.targetHighPrice IS NOT NULL OR cw.targetLowPrice IS NOT NULL " +
           "GROUP BY cw.coinId")
    List<Object[]> findAverageTargetPricesByCoin();
    
    /**
     * 특정 코인을 관심종목으로 등록한 사용자 ID 목록 조회 (알림 발송용)
     */
    @Query("SELECT cw.user.id FROM CoinWatchlist cw WHERE cw.coinId = :coinId")
    List<Long> findUserIdsByCoinId(@Param("coinId") String coinId);
}
