package com.coincommunity.backend.repository;

import com.coincommunity.backend.dto.PriceAlertDto;
import com.coincommunity.backend.entity.PriceAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 가격 알림 데이터 액세스 인터페이스
 */
@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    /**
     * 특정 사용자의 가격 알림 목록을 페이징하여 조회
     */
    Page<PriceAlert> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 특정 코인에 대한 가격 알림 목록 조회
     */
    List<PriceAlert> findByUserIdAndSymbol(Long userId, String symbol);

    /**
     * 특정 상태의 가격 알림 목록 조회
     */
    List<PriceAlert> findByStatus(PriceAlertDto.AlertStatus status);

    /**
     * 특정 코인에 대한 대기 중인 가격 알림 목록 조회
     */
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.symbol = :symbol AND pa.status = 'PENDING'")
    List<PriceAlert> findPendingAlertsBySymbol(@Param("symbol") String symbol);

    /**
     * 특정 사용자의 대기 중인 가격 알림 목록 조회
     */
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.user.id = :userId AND pa.status = 'PENDING'")
    List<PriceAlert> findPendingAlertsByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 코인에 대한 대기 중인 가격 알림 목록 조회
     */
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.user.id = :userId AND pa.symbol = :symbol AND pa.status = 'PENDING'")
    List<PriceAlert> findPendingAlertsByUserIdAndSymbol(@Param("userId") Long userId, @Param("symbol") String symbol);

    /**
     * 특정 사용자의 가격 알림 개수 조회
     */
    long countByUserId(Long userId);

    /**
     * 특정 사용자의 특정 상태의 가격 알림 개수 조회
     */
    long countByUserIdAndStatus(Long userId, PriceAlertDto.AlertStatus status);
}