package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 거래 내역 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 시계열 데이터 최적화
 * - 트랜잭션 분석 쿼리
 * - 대용량 데이터 처리 고려
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 사용자의 거래 내역 조회 (최신순)
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserIdOrderByTransactionDateDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 포트폴리오별 거래 내역 조회
     */
    @Query("SELECT t FROM Transaction t WHERE t.portfolio.id = :portfolioId ORDER BY t.transactionDate DESC")
    List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오별 거래 내역 조회 (페이징)
     */
    @Query("SELECT t FROM Transaction t WHERE t.portfolio.id = :portfolioId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByPortfolioIdOrderByTransactionDateDesc(@Param("portfolioId") Long portfolioId, Pageable pageable);

    /**
     * 특정 코인의 거래 내역 조회
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.coinId = :coinId ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndCoinIdOrderByTransactionDateDesc(@Param("userId") Long userId, @Param("coinId") String coinId);

    /**
     * 매수 거래만 조회 (평균가 계산용)
     */
    @Query("SELECT t FROM Transaction t WHERE t.portfolio.id = :portfolioId AND t.coinId = :coinId AND t.type = 'BUY' ORDER BY t.transactionDate ASC")
    List<Transaction> findBuyTransactionsByPortfolioAndCoin(@Param("portfolioId") Long portfolioId, @Param("coinId") String coinId);

    /**
     * 매도 거래만 조회 (실현손익 분석용)
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type = 'SELL' ORDER BY t.transactionDate DESC")
    List<Transaction> findSellTransactionsByUserId(@Param("userId") Long userId);

    /**
     * 특정 기간의 거래 내역 조회
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndTransactionDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 일일 거래량 집계
     */
    @Query("SELECT DATE(t.transactionDate) as tradeDate, SUM(t.totalAmount) as dailyVolume " +
           "FROM Transaction t WHERE t.user.id = :userId " +
           "GROUP BY DATE(t.transactionDate) ORDER BY tradeDate DESC")
    List<Object[]> findDailyTradingVolumeByUserId(@Param("userId") Long userId);

    /**
     * 코인별 거래 통계
     */
    @Query("SELECT t.coinId, t.coinName, " +
           "COUNT(CASE WHEN t.type = 'BUY' THEN 1 END) as buyCount, " +
           "COUNT(CASE WHEN t.type = 'SELL' THEN 1 END) as sellCount, " +
           "SUM(CASE WHEN t.type = 'BUY' THEN t.totalAmount ELSE 0 END) as buyVolume, " +
           "SUM(CASE WHEN t.type = 'SELL' THEN t.totalAmount ELSE 0 END) as sellVolume " +
           "FROM Transaction t WHERE t.user.id = :userId " +
           "GROUP BY t.coinId, t.coinName ORDER BY buyVolume DESC")
    List<Object[]> findTradingStatsByUserId(@Param("userId") Long userId);

    /**
     * 월별 거래 통계
     */
    @Query("SELECT YEAR(t.transactionDate) as year, MONTH(t.transactionDate) as month, " +
           "COUNT(t) as transactionCount, SUM(t.totalAmount) as totalVolume " +
           "FROM Transaction t WHERE t.user.id = :userId " +
           "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyTradingStatsByUserId(@Param("userId") Long userId);

    /**
     * 실현손익 조회
     */
    @Query("SELECT SUM(t.realizedGain) FROM Transaction t WHERE t.user.id = :userId AND t.realizedGain IS NOT NULL")
    Optional<BigDecimal> findTotalRealizedGainByUserId(@Param("userId") Long userId);

    /**
     * 최근 거래 내역 조회 (대시보드용)
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 거래소별 거래 통계
     */
    @Query("SELECT t.exchange, COUNT(t) as transactionCount, SUM(t.totalAmount) as totalVolume " +
           "FROM Transaction t WHERE t.user.id = :userId " +
           "GROUP BY t.exchange ORDER BY totalVolume DESC")
    List<Object[]> findExchangeStatsByUserId(@Param("userId") Long userId);

    /**
     * 총 거래 횟수 조회
     */
    long countByUserId(Long userId);

    /**
     * 특정 코인의 총 매수량 계산
     */
    @Query("SELECT SUM(t.quantity) FROM Transaction t WHERE t.portfolio.id = :portfolioId AND t.coinId = :coinId AND t.type = 'BUY'")
    Optional<BigDecimal> findTotalBuyQuantityByPortfolioAndCoin(@Param("portfolioId") Long portfolioId, @Param("coinId") String coinId);

    /**
     * 특정 코인의 총 매도량 계산
     */
    @Query("SELECT SUM(t.quantity) FROM Transaction t WHERE t.portfolio.id = :portfolioId AND t.coinId = :coinId AND t.type = 'SELL'")
    Optional<BigDecimal> findTotalSellQuantityByPortfolioAndCoin(@Param("portfolioId") Long portfolioId, @Param("coinId") String coinId);

    /**
     * 평균 거래 단가 계산 (매수)
     */
    @Query("SELECT AVG(t.price) FROM Transaction t WHERE t.portfolio.id = :portfolioId AND t.coinId = :coinId AND t.type = 'BUY'")
    Optional<BigDecimal> findAverageBuyPriceByPortfolioAndCoin(@Param("portfolioId") Long portfolioId, @Param("coinId") String coinId);

    /**
     * 대용량 거래 조회 (VIP 사용자 분석용)
     */
    @Query("SELECT t FROM Transaction t WHERE t.totalAmount >= :minAmount ORDER BY t.totalAmount DESC")
    List<Transaction> findLargeTransactions(@Param("minAmount") BigDecimal minAmount, Pageable pageable);

    /**
     * 거래 빈도 분석 (활성 사용자 식별)
     */
    @Query("SELECT t.user.id, COUNT(t) as transactionCount " +
           "FROM Transaction t WHERE t.transactionDate >= :since " +
           "GROUP BY t.user.id ORDER BY transactionCount DESC")
    List<Object[]> findMostActiveTraders(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 수수료 통계
     */
    @Query("SELECT SUM(t.fee) FROM Transaction t WHERE t.user.id = :userId")
    Optional<BigDecimal> findTotalFeeByUserId(@Param("userId") Long userId);

    /**
     * 포트폴리오 ID로 트랜잭션 삭제
     */
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@Param("portfolioId") Long portfolioId);
}
