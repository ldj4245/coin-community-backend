package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.NotificationStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 통계 정보 관리를 위한 Repository 인터페이스
 * 알림 전송 통계 관련 데이터베이스 작업을 처리합니다.
 */
@Repository
public interface NotificationStatisticsRepository extends JpaRepository<NotificationStatistics, Long> {

    /**
     * 알림 타입, 전송 방법, 시간으로 통계 조회
     */
    Optional<NotificationStatistics> findByNotificationTypeAndDeliveryMethodAndDateHour(
            String notificationType, String deliveryMethod, LocalDateTime dateHour);

    /**
     * 특정 시간 범위의 통계 조회
     */
    List<NotificationStatistics> findByDateHourBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 알림 타입별 통계 조회
     */
    List<NotificationStatistics> findByNotificationType(String notificationType);

    /**
     * 전송 방법별 통계 조회
     */
    List<NotificationStatistics> findByDeliveryMethod(String deliveryMethod);

    /**
     * 최근 24시간 통계 조회
     */
    @Query("SELECT ns FROM NotificationStatistics ns WHERE ns.dateHour >= :since ORDER BY ns.dateHour DESC")
    List<NotificationStatistics> findRecentStatistics(@Param("since") LocalDateTime since);

    /**
     * 알림 타입별 총 성공/실패 카운트 조회
     */
    @Query("SELECT ns.notificationType, SUM(ns.successCount), SUM(ns.failureCount), SUM(ns.totalCount) " +
           "FROM NotificationStatistics ns GROUP BY ns.notificationType")
    List<Object[]> findTotalStatsByType();

    /**
     * 전송 방법별 총 성공/실패 카운트 조회
     */
    @Query("SELECT ns.deliveryMethod, SUM(ns.successCount), SUM(ns.failureCount), SUM(ns.totalCount) " +
           "FROM NotificationStatistics ns GROUP BY ns.deliveryMethod")
    List<Object[]> findTotalStatsByDeliveryMethod();

    /**
     * 시간별 성공률 통계 조회
     */
    @Query("SELECT ns.dateHour, AVG(CASE WHEN ns.totalCount > 0 THEN CAST(ns.successCount AS DOUBLE) / ns.totalCount * 100 ELSE 0 END) " +
           "FROM NotificationStatistics ns WHERE ns.dateHour >= :since GROUP BY ns.dateHour ORDER BY ns.dateHour")
    List<Object[]> findSuccessRateByHour(@Param("since") LocalDateTime since);

    /**
     * 오래된 통계 데이터 삭제 (데이터 정리용)
     */
    void deleteByDateHourBefore(LocalDateTime cutoffDate);
}
