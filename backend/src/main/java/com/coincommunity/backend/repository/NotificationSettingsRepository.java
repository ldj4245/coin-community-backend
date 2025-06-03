package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 설정 레포지토리
 * 사용자별 알림 설정을 관리합니다.
 */
@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    
    /**
     * 사용자 ID로 알림 설정 조회
     */
    Optional<NotificationSettings> findByUserId(Long userId);
    
    /**
     * 사용자 ID로 알림 설정 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}