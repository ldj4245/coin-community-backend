package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.AnalysisLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 분석 좋아요 정보 관리를 위한 Repository 인터페이스
 * 코인 분석 좋아요 관련 데이터베이스 작업을 처리합니다.
 */
@Repository
public interface AnalysisLikeRepository extends JpaRepository<AnalysisLike, Long> {

    /**
     * 사용자 ID와 분석 ID로 좋아요 존재 여부 확인
     */
    boolean existsByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 사용자 ID와 분석 ID로 좋아요 조회
     */
    Optional<AnalysisLike> findByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 사용자 ID로 모든 좋아요 조회 (최신순)
     */
    @Query("SELECT al FROM AnalysisLike al JOIN FETCH al.analysis a JOIN FETCH a.user WHERE al.user.id = :userId ORDER BY al.createdAt DESC")
    List<AnalysisLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 분석 ID로 모든 좋아요 조회
     */
    List<AnalysisLike> findByAnalysisId(Long analysisId);

    /**
     * 분석 ID로 좋아요 수 계산
     */
    long countByAnalysisId(Long analysisId);

    /**
     * 사용자 ID로 좋아요 수 계산
     */
    long countByUserId(Long userId);

    /**
     * 사용자 ID와 분석 ID로 좋아요 삭제
     */
    void deleteByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 분석 ID로 모든 좋아요 삭제 (분석 삭제 시 사용)
     */
    void deleteByAnalysisId(Long analysisId);

    /**
     * 사용자 ID로 모든 좋아요 삭제 (사용자 삭제 시 사용)
     */
    void deleteByUserId(Long userId);

    /**
     * 인기 분석 조회를 위한 좋아요 수 기준 분석 ID 조회
     */
    @Query("SELECT l.analysis.id, COUNT(l) FROM AnalysisLike l " +
           "GROUP BY l.analysis.id ORDER BY COUNT(l) DESC")
    List<Object[]> findTopAnalysesByLikeCount();
}
