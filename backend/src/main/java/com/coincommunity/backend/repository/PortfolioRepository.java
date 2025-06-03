package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 레포지토리
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 복잡한 쿼리 최적화
 * - 캐싱 친화적 메서드 설계
 * - 성능 모니터링 고려
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * 사용자의 모든 포트폴리오 조회 (기본 포트폴리오 우선 정렬)
     */
    @Query("SELECT p FROM Portfolio p WHERE p.user.id = :userId ORDER BY p.isDefault DESC, p.createdAt DESC")
    List<Portfolio> findByUserIdOrderByIsDefaultDescCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자의 기본 포트폴리오 조회
     */
    Optional<Portfolio> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * 공개 포트폴리오 목록 조회 (수익률 순 정렬)
     */
    @Query("SELECT p FROM Portfolio p WHERE p.isPublic = true ORDER BY p.totalReturnPercent DESC")
    Page<Portfolio> findByIsPublicTrueOrderByTotalReturnPercentDesc(Pageable pageable);

    /**
     * 상위 수익률 포트폴리오 조회 (랭킹용)
     */
    @Query("SELECT p FROM Portfolio p WHERE p.isPublic = true AND p.totalInvestment > :minInvestment " +
           "ORDER BY p.totalReturnPercent DESC")
    Page<Portfolio> findTopPerformingPortfolios(@Param("minInvestment") BigDecimal minInvestment, Pageable pageable);

    /**
     * 사용자 포트폴리오 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 공개 포트폴리오 수 조회
     */
    long countByIsPublicTrue();

    /**
     * 특정 기간 내 생성된 포트폴리오 조회
     */
    @Query("SELECT p FROM Portfolio p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Portfolio> findByCreatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * 평균 수익률 계산
     */
    @Query("SELECT AVG(p.totalReturnPercent) FROM Portfolio p WHERE p.isPublic = true")
    Optional<BigDecimal> findAverageReturnPercent();

    /**
     * 특정 수익률 이상의 포트폴리오 조회
     */
    @Query("SELECT p FROM Portfolio p WHERE p.isPublic = true AND p.totalReturnPercent >= :minReturn " +
           "ORDER BY p.totalReturnPercent DESC")
    List<Portfolio> findByTotalReturnPercentGreaterThanEqual(@Param("minReturn") BigDecimal minReturn);

    /**
     * 사용자의 포트폴리오 통계 조회
     */
    @Query("SELECT " +
           "COUNT(p) as portfolioCount, " +
           "SUM(p.totalInvestment) as totalInvestment, " +
           "SUM(p.currentValue) as totalCurrentValue, " +
           "AVG(p.totalReturnPercent) as avgReturnPercent " +
           "FROM Portfolio p WHERE p.user.id = :userId")
    Object[] findPortfolioStatsByUserId(@Param("userId") Long userId);

    /**
     * 포트폴리오 이름 중복 체크 (같은 사용자 내)
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * 사용자의 활성 포트폴리오 조회 (아이템이 있는 포트폴리오)
     */
    @Query("SELECT DISTINCT p FROM Portfolio p JOIN p.items i WHERE p.user.id = :userId")
    List<Portfolio> findActivePortfoliosByUserId(@Param("userId") Long userId);

    /**
     * 사용자 포트폴리오를 기본 여부와 생성일 순으로 조회
     */
    List<Portfolio> findByUserIdOrderByIsDefaultDescCreatedAtAsc(Long userId);
}
