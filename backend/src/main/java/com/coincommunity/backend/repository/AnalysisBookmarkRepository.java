package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.AnalysisBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 분석 북마크 정보 관리를 위한 Repository 인터페이스
 * 코인 분석 북마크 관련 데이터베이스 작업을 처리합니다.
 */
@Repository
public interface AnalysisBookmarkRepository extends JpaRepository<AnalysisBookmark, Long> {

    /**
     * 사용자 ID와 분석 ID로 북마크 존재 여부 확인
     */
    boolean existsByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 사용자 ID와 분석 ID로 북마크 조회
     */
    Optional<AnalysisBookmark> findByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 사용자 ID로 모든 북마크 조회 (최신순)
     */
    @Query("SELECT ab FROM AnalysisBookmark ab JOIN FETCH ab.analysis a JOIN FETCH a.user ORDER BY ab.createdAt DESC")
    List<AnalysisBookmark> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 분석 ID로 모든 북마크 조회
     */
    List<AnalysisBookmark> findByAnalysisId(Long analysisId);

    /**
     * 분석 ID로 북마크 수 계산
     */
    long countByAnalysisId(Long analysisId);

    /**
     * 사용자 ID로 북마크 수 계산
     */
    long countByUserId(Long userId);

    /**
     * 사용자 ID와 분석 ID로 북마크 삭제
     */
    void deleteByUserIdAndAnalysisId(Long userId, Long analysisId);

    /**
     * 분석 ID로 모든 북마크 삭제 (분석 삭제 시 사용)
     */
    void deleteByAnalysisId(Long analysisId);

    /**
     * 사용자 ID로 모든 북마크 삭제 (사용자 삭제 시 사용)
     */
    void deleteByUserId(Long userId);
}
