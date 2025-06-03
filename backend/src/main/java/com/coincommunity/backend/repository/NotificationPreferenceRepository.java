package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 정보 관리를 위한 Repository 인터페이스
 * 사용자별 알림 설정 관련 데이터베이스 작업을 처리합니다.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * 사용자 ID와 알림 타입으로 설정 조회
     */
    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, String notificationType);

    /**
     * 사용자 ID로 모든 알림 설정 조회
     */
    List<NotificationPreference> findByUserId(Long userId);

    /**
     * 활성화된 알림 설정만 조회
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.user.id = :userId AND np.isEnabled = true")
    List<NotificationPreference> findEnabledByUserId(@Param("userId") Long userId);

    /**
     * 특정 알림 타입이 활성화된 사용자 ID 목록 조회
     */
    @Query("SELECT np.user.id FROM NotificationPreference np WHERE np.notificationType = :type AND np.isEnabled = true")
    List<Long> findUserIdsByEnabledNotificationType(@Param("type") String notificationType);

    /**
     * 푸시 알림이 활성화된 사용자들의 설정 조회
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.notificationType = :type AND np.isEnabled = true AND np.pushEnabled = true")
    List<NotificationPreference> findByTypeAndPushEnabled(@Param("type") String notificationType);

    /**
     * 웹소켓 알림이 활성화된 사용자들의 설정 조회
     */
    @Query("SELECT np FROM NotificationPreference np WHERE np.notificationType = :type AND np.isEnabled = true AND np.websocketEnabled = true")
    List<NotificationPreference> findByTypeAndWebSocketEnabled(@Param("type") String notificationType);

    /**
     * 사용자 ID와 알림 타입으로 활성화 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(np) > 0 THEN true ELSE false END FROM NotificationPreference np " +
           "WHERE np.user.id = :userId AND np.notificationType = :type AND np.isEnabled = true")
    boolean isNotificationEnabled(@Param("userId") Long userId, @Param("type") String notificationType);

    /**
     * 사용자의 모든 알림 설정 삭제
     */
    void deleteByUserId(Long userId);
}
