package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.UserScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 점수 레포지토리 (간소화)
 * 코인 커뮤니티에 필요한 핵심 기능만 제공
 */
@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, Long> {

    /**
     * 사용자별 점수 조회
     */
    Optional<UserScore> findByUserId(Long userId);

    /**
     * 전체 사용자 랭킹 조회 (총점 기준)
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.totalScore DESC")
    Page<UserScore> findAllByOrderByTotalScoreDesc(Pageable pageable);

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
    @Query("SELECT us FROM UserScore us WHERE us.isVerified = true ORDER BY us.totalScore DESC")
    List<UserScore> findByIsExpertTrue();

    /**
     * 레벨별 사용자 수 통계
     */
    @Query("SELECT us.level, COUNT(us) FROM UserScore us GROUP BY us.level ORDER BY us.level")
    List<Object[]> countUsersByLevel();

    /**
     * 평균 점수 조회
     */
    @Query("SELECT AVG(us.totalScore) FROM UserScore us")
    Double findAverageTotalScore();

    /**
     * 최근 생성된 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.createdAt >= :since ORDER BY us.createdAt DESC")
    List<UserScore> findRecentlyCreated(@Param("since") LocalDateTime since);

    /**
     * 사용자 순위 조회 (특정 사용자의 랭킹)
     */
    @Query("SELECT COUNT(us) + 1 FROM UserScore us WHERE us.totalScore > " +
           "(SELECT us2.totalScore FROM UserScore us2 WHERE us2.user.id = :userId)")
    Optional<Long> findUserRank(@Param("userId") Long userId);

    /**
     * 최고 점수 기록 조회
     */
    @Query("SELECT MAX(us.totalScore) FROM UserScore us")
    Optional<Integer> findMaxTotalScore();

    /**
     * 점수 분포 통계
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN us.totalScore < 100 THEN 1 END) as beginner, " +
           "COUNT(CASE WHEN us.totalScore BETWEEN 100 AND 499 THEN 1 END) as intermediate, " +
           "COUNT(CASE WHEN us.totalScore BETWEEN 500 AND 999 THEN 1 END) as advanced, " +
           "COUNT(CASE WHEN us.totalScore >= 1000 THEN 1 END) as expert " +
           "FROM UserScore us")
    Object[] findScoreDistribution();

    /**
     * 신규 사용자 조회 (레벨 1, 최근 생성)
     */
    @Query("SELECT us FROM UserScore us WHERE us.level = 1 AND us.createdAt >= :since ORDER BY us.createdAt DESC")
    List<UserScore> findNewUsers(@Param("since") LocalDateTime since);

    /**
     * 특정 점수보다 높은 사용자 수 조회 (랭킹 계산용)
     */
    long countByTotalScoreGreaterThan(Integer totalScore);
    
    /**
     * 특정 레벨의 사용자 수 조회
     */
    long countByLevel(Integer level);
    
    /**
     * 상위 10명 사용자 조회
     */
    @Query("SELECT us FROM UserScore us ORDER BY us.totalScore DESC LIMIT 10")
    List<UserScore> findTop10ByOrderByTotalScoreDesc();
    
    /**
     * 전문가 인증 사용자 페이징 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.isVerified = true ORDER BY us.totalScore DESC")
    Page<UserScore> findByIsExpertTrue(Pageable pageable);

    /**
     * 분석 활동이 활발한 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.analysisCount > 0 ORDER BY us.analysisCount DESC, us.analysisAccuracyRate DESC")
    List<UserScore> findActiveAnalysts(Pageable pageable);

    /**
     * 커뮤니티 활동이 활발한 사용자 조회
     */
    @Query("SELECT us FROM UserScore us WHERE (us.postCount + us.commentCount) > 0 ORDER BY (us.postCount + us.commentCount) DESC")
    List<UserScore> findActiveCommunityMembers(Pageable pageable);

    /**
     * 정확도가 높은 분석가 조회
     */
    @Query("SELECT us FROM UserScore us WHERE us.analysisCount >= 5 AND us.analysisAccuracyRate >= :minAccuracy ORDER BY us.analysisAccuracyRate DESC")
    List<UserScore> findAccurateAnalysts(@Param("minAccuracy") Double minAccuracy);
}
