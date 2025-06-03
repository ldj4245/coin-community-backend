package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 저장소
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 목록 조회
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 특정 기간 내 알림 조회
     */
    List<Notification> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
            
    /**
     * 특정 타입의 알림 조회
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
    
    /**
     * 읽지 않은 알림 갯수 조회
     */
    long countByUserIdAndIsReadFalse(Long userId);
    
    /**
     * 사용자별 특정 기간 내 알림 갯수 조회
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 오늘 발송된 알림 수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND DATE(n.createdAt) = CURRENT_DATE")
    long countTodayNotificationsByUserId(@Param("userId") Long userId);
}
