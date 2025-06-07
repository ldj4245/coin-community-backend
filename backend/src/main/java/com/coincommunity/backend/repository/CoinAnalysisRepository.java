package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.CoinAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 코인 분석 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 복잡한 분석 데이터 쿼리 최적화
 * - 사용자 참여도 기반 랭킹
 * - 예측 정확도 추적 시스템
 */
@Repository
public interface CoinAnalysisRepository extends JpaRepository<CoinAnalysis, Long> {

    /**
     * 코인별 분석글 조회 (최신순)
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.coinId = :coinId ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByCoinIdOrderByCreatedAtDesc(@Param("coinId") String coinId, Pageable pageable);

    /**
     * 사용자의 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.user.id = :userId ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 인기 분석글 조회 (좋아요 수 기준)
     */
    @Query("SELECT ca FROM CoinAnalysis ca ORDER BY ca.likeCount DESC, ca.createdAt DESC")
    Page<CoinAnalysis> findByOrderByLikeCountDescCreatedAtDesc(Pageable pageable);

    /**
     * 추천 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.isFeatured = true ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByIsFeaturedTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 검증된 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.isVerified = true ORDER BY ca.accuracyScore DESC NULLS LAST")
    Page<CoinAnalysis> findByIsVerifiedTrueOrderByAccuracyScoreDesc(Pageable pageable);

    /**
     * 분석 유형별 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.analysisType = :analysisType ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByAnalysisTypeOrderByCreatedAtDesc(@Param("analysisType") CoinAnalysis.AnalysisType analysisType, Pageable pageable);

    /**
     * 예측 기간별 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.predictionPeriod = :predictionPeriod ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByPredictionPeriodOrderByCreatedAtDesc(@Param("predictionPeriod") CoinAnalysis.PredictionPeriod predictionPeriod, Pageable pageable);

    /**
     * 투자 추천도별 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.recommendation = :recommendation ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByRecommendationOrderByCreatedAtDesc(@Param("recommendation") CoinAnalysis.InvestmentRecommendation recommendation, Pageable pageable);

    /**
     * 특정 기간의 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.createdAt BETWEEN :startDate AND :endDate ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 높은 정확도의 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.accuracyScore >= :minAccuracy ORDER BY ca.accuracyScore DESC")
    List<CoinAnalysis> findByAccuracyScoreGreaterThanEqual(@Param("minAccuracy") BigDecimal minAccuracy);

    /**
     * 사용자별 분석 통계
     */
    @Query("SELECT " +
           "COUNT(ca) as totalAnalysis, " +
           "AVG(ca.likeCount) as avgLikes, " +
           "AVG(ca.viewCount) as avgViews, " +
           "AVG(ca.accuracyScore) as avgAccuracy " +
           "FROM CoinAnalysis ca WHERE ca.user.id = :userId")
    Object[] findAnalysisStatsByUserId(@Param("userId") Long userId);

    /**
     * 코인별 분석 통계
     */
    @Query("SELECT ca.coinId, ca.coinName, COUNT(ca) as analysisCount, AVG(ca.accuracyScore) as avgAccuracy " +
           "FROM CoinAnalysis ca GROUP BY ca.coinId, ca.coinName ORDER BY analysisCount DESC")
    List<Object[]> findAnalysisStatsByCoin();

    /**
     * 최근 인기 분석글 (조회수 + 좋아요 종합)
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.createdAt >= :since " +
           "ORDER BY (ca.viewCount + ca.likeCount * 5) DESC")
    List<CoinAnalysis> findTrendingAnalysis(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 태그로 분석글 검색
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.tags LIKE %:tag% ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findByTagsContaining(@Param("tag") String tag);

    /**
     * 제목이나 내용으로 검색
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.title LIKE %:keyword% OR ca.content LIKE %:keyword% ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 확신도별 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.confidenceLevel >= :minConfidence ORDER BY ca.confidenceLevel DESC, ca.createdAt DESC")
    List<CoinAnalysis> findByConfidenceLevelGreaterThanEqual(@Param("minConfidence") Integer minConfidence);

    /**
     * 위험도별 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.riskLevel <= :maxRisk ORDER BY ca.riskLevel ASC, ca.createdAt DESC")
    List<CoinAnalysis> findByRiskLevelLessThanEqual(@Param("maxRisk") Integer maxRisk);

    /**
     * 사용자가 좋아요한 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.id IN " +
           "(SELECT l.analysis.id FROM AnalysisLike l WHERE l.user.id = :userId) " +
           "ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findLikedAnalysisByUserId(@Param("userId") Long userId);

    /**
     * 북마크된 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.id IN " +
           "(SELECT b.analysis.id FROM AnalysisBookmark b WHERE b.user.id = :userId) " +
           "ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findBookmarkedAnalysisByUserId(@Param("userId") Long userId);

    /**
     * 예측 결과가 나온 분석글 조회 (정확도 계산 완료)
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.accuracyScore IS NOT NULL ORDER BY ca.accuracyScore DESC")
    List<CoinAnalysis> findAnalysisWithAccuracyScore();

    /**
     * 월별 분석글 수 통계
     */
    @Query("SELECT YEAR(ca.createdAt) as year, MONTH(ca.createdAt) as month, COUNT(ca) as count " +
           "FROM CoinAnalysis ca GROUP BY YEAR(ca.createdAt), MONTH(ca.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyAnalysisCount();

    /**
     * 전문가(검증된) 사용자의 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.user.id IN " +
           "(SELECT us.user.id FROM UserScore us WHERE us.isVerified = true) " +
           "ORDER BY ca.createdAt DESC")
    Page<CoinAnalysis> findExpertAnalysis(Pageable pageable);

    /**
     * 특정 목표가 범위의 분석글 조회
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.targetPrice BETWEEN :minPrice AND :maxPrice ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findByTargetPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * 사용자의 총 분석글 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 사용자의 분석글 조회 (List 형태)
     */
    @Query("SELECT ca FROM CoinAnalysis ca WHERE ca.user.id = :userId ORDER BY ca.createdAt DESC")
    List<CoinAnalysis> findByUserId(@Param("userId") Long userId);
}
