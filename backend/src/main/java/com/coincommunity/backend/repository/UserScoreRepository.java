package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.UserScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 점수 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 사용자 레벨링 시스템 최적화
 * - 리더보드 성능 최적화
 * - 점수 이력 추적 시스템
 */
@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, Long> {

    /**
     * 사용자별 점수 조회
     */
    Optional<UserScore> findByUserId(Long userId);

    /**
     * 전체 사용자 랭킹 조회 (총점과 레벨, 생성일 기준 정렬)
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.totalScore DESC, us.level DESC, us.createdAt ASC")
    Page<UserScore> findAllByOrderByTotalScoreDescLevelDescCreatedAtAsc(Pageable pageable);

    /**
     * 활동 점수 랭킹 조회 (커뮤니티 점수 기준)
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.communityScore DESC")
    Page<UserScore> findAllOrderByActivityScoreDesc(Pageable pageable);

    /**
     * 포트폴리오 점수 랭킹 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.portfolioScore DESC")
    Page<UserScore> findAllOrderByPortfolioScoreDesc(Pageable pageable);

    /**
     * 예측 정확도 랭킹 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.predictionScore DESC")
    Page<UserScore> findAllOrderByPredictionScoreDesc(Pageable pageable);

    /**
     * 커뮤니티 기여도 랭킹 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.communityScore DESC")
    Page<UserScore> findAllOrderByCommunityScoreDesc(Pageable pageable);

    /**
     * 특정 레벨의 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.level = :level ORDER BY us.totalScore DESC")
    List<UserScore> findByLevel(@Param("level") Integer level);

    /**
     * 특정 레벨 이상의 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.level >= :minLevel ORDER BY us.level DESC, us.totalScore DESC")
    List<UserScore> findByLevelGreaterThanEqual(@Param("minLevel") Integer minLevel);

    /**
     * 전문가 인증 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.isExpert = true ORDER BY us.totalScore DESC")
    List<UserScore> findByIsExpertTrue();

    /**
     * 레벨별 사용자 수 통계
     */
    @Query("SELECT us.level, COUNT(us) FROM UserScore us GROUP BY us.level ORDER BY us.level")
    List<Object[]> countUsersByLevel();

    /**
     * 평균 점수 조회
     */
    @Query("SELECT " +
           "AVG(us.totalScore) as avgTotal, " +
           "AVG(us.communityScore) as avgActivity, " +
           "AVG(us.portfolioScore) as avgPortfolio, " +
           "AVG(us.predictionScore) as avgPrediction, " +
           "AVG(us.communityScore) as avgCommunity " +
           "FROM UserScore us")
    Object[] findAverageScores();

    /**
     * 상위 퍼센트 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.totalScore >= " +
           "(SELECT DISTINCT us2.totalScore FROM UserScore us2 ORDER BY us2.totalScore DESC " +
           "OFFSET :offset ROWS FETCH FIRST 1 ROWS ONLY) ORDER BY us.totalScore DESC")
    List<UserScore> findTopPercentUsers(@Param("offset") Integer offset);

    /**
     * 최근 점수 업데이트된 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.lastUpdatedAt >= :since ORDER BY us.lastUpdatedAt DESC")
    List<UserScore> findRecentlyUpdated(@Param("since") LocalDateTime since);

    /**
     * 특정 기간 내 레벨업한 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.lastLevelUpAt BETWEEN :startDate AND :endDate ORDER BY us.lastLevelUpAt DESC")
    List<UserScore> findLevelUpUsers(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자 순위 조회 (특정 사용자의 랭킹)
     */
    @Query("SELECT COUNT(us) + 1 FROM UserScore us WHERE us.totalScore > " +
           "(SELECT us2.totalScore FROM UserScore us2 WHERE us2.user.id = :userId)")
    Optional<Long> findUserRank(@Param("userId") Long userId);

    /**
     * 주변 랭킹 사용자 조회 (상하 N명)
     */
    @Query("SELECT us FROM UserScore us WHERE us.totalScore > " +
           "(SELECT us2.totalScore FROM UserScore us2 WHERE us2.user.id = :userId) " +
           "ORDER BY us.totalScore ASC")
    List<UserScore> findHigherRankedUsers(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT us FROM UserScore us WHERE us.totalScore < " +
           "(SELECT us2.totalScore FROM UserScore us2 WHERE us2.user.id = :userId) " +
           "ORDER BY us.totalScore DESC")
    List<UserScore> findLowerRankedUsers(@Param("userId") Long userId, Pageable pageable);

    /**
     * 최고 점수 기록 조회
     */
    @Query("SELECT MAX(us.totalScore) FROM UserScore us")
    Optional<BigDecimal> findMaxTotalScore();

    /**
     * 월별 레벨업 통계
     */
    @Query("SELECT YEAR(us.lastLevelUpAt) as year, MONTH(us.lastLevelUpAt) as month, COUNT(us) as levelUpCount " +
           "FROM UserScore us WHERE us.lastLevelUpAt IS NOT NULL " +
           "GROUP BY YEAR(us.lastLevelUpAt), MONTH(us.lastLevelUpAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyLevelUpStats();

    /**
     * 점수 분포 통계
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN us.totalScore < 1000 THEN 1 END) as under1k, " +
           "COUNT(CASE WHEN us.totalScore BETWEEN 1000 AND 4999 THEN 1 END) as between1k5k, " +
           "COUNT(CASE WHEN us.totalScore BETWEEN 5000 AND 9999 THEN 1 END) as between5k10k, " +
           "COUNT(CASE WHEN us.totalScore >= 10000 THEN 1 END) as over10k " +
           "FROM UserScore us")
    Object[] findScoreDistribution();

    /**
     * 신규 사용자 조회 (레벨 1, 최근 생성)
     */
    @Query("SELECT us FROM UserScore us WHERE us.level = 1 AND us.createdAt >= :since ORDER BY us.createdAt DESC")
    List<UserScore> findNewUsers(@Param("since") LocalDateTime since);

    /**
     * 비활성 사용자 조회 (오랫동안 점수 업데이트 없음)
     */
    @Query("SELECT us FROM UserScore us WHERE us.lastUpdatedAt < :threshold ORDER BY us.lastUpdatedAt ASC")
    List<UserScore> findInactiveUsers(@Param("threshold") LocalDateTime threshold);

    /**
     * 연속 활동일수 상위 사용자 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.consecutiveDays DESC")
    List<UserScore> findTopConsecutiveActiveUsers(Pageable pageable);

    /**
     * 특정 점수 범위의 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.totalScore BETWEEN :minScore AND :maxScore ORDER BY us.totalScore DESC")
    List<UserScore> findByTotalScoreBetween(@Param("minScore") BigDecimal minScore, @Param("maxScore") BigDecimal maxScore);

    /**
     * 다음 레벨까지 필요한 점수가 적은 사용자 조회 (레벨업 임박)
     */
    @Query("SELECT us FROM UserScore us WHERE (us.nextLevelRequiredScore - us.totalScore) <= :threshold AND us.nextLevelRequiredScore > us.totalScore ORDER BY (us.nextLevelRequiredScore - us.totalScore) ASC")
    List<UserScore> findUsersNearLevelUp(@Param("threshold") BigDecimal threshold);

    /**
     * 특정 점수보다 높은 사용자 수 조회 (랭킹 계산용)
     */
    long countByTotalScoreGreaterThan(Integer totalScore);
    
    /**
     * 특정 레벨의 사용자 수 조회
     */
    long countByLevel(Integer level);
    
    /**
     * 전체 사용자 랭킹 조회 - 총점 기준
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.totalScore DESC")
    Page<UserScore> findAllByOrderByTotalScoreDesc(Pageable pageable);
    
    /**
     * 전체 사용자 랭킹 조회 - 분석점수 기준
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.analysisScore DESC")
    Page<UserScore> findAllByOrderByAnalysisScoreDesc(Pageable pageable);
    
    /**
     * 전체 사용자 랭킹 조회 - 커뮤니티점수 기준
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.communityScore DESC")
    Page<UserScore> findAllByOrderByCommunityScoreDesc(Pageable pageable);
    
    /**
     * 전체 사용자 랭킹 조회 - 거래점수 기준
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.tradingScore DESC")
    Page<UserScore> findAllByOrderByTradingScoreDesc(Pageable pageable);
    
    /**
     * 상위 10명 사용자 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.totalScore DESC LIMIT 10")
    List<UserScore> findTop10ByOrderByTotalScoreDesc();
    
    /**
     * 전문가 인증 사용자 페이징 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.isExpert = true ORDER BY us.totalScore DESC")
    Page<UserScore> findByIsExpertTrue(Pageable pageable);
}
