package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.NotificationDto;
import com.coincommunity.backend.entity.Notification;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.NotificationRepository;
import com.coincommunity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 알림 서비스
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 실시간 WebSocket 알림 전송
 * - 푸시 알림 시스템 연동
 * - 알림 배치 처리 최적화
 * - 사용자별 알림 설정 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    @Lazy
    private final RealtimeNotificationService realtimeNotificationService;
    
    /**
     * 단일 사용자에게 알림 생성 및 실시간 전송
     */
    @Transactional
    public NotificationDto.Response createNotification(NotificationDto.CreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        
        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .resourceId(request.getResourceId())
                .isRead(false)
                .build();
        
        notification = notificationRepository.save(notification);
        
        return NotificationDto.Response.from(notification);
    }
    
    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getNotifications(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationDto.Response::from);
    }
    
    /**
     * 읽지 않은 알림 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationDto.Response> getUnreadNotifications(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        return NotificationDto.Response.from(
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId));
    }
    
    /**
     * 알림 상태 요약 조회 (읽지 않은 알림 개수 + 최신 알림)
     */
    @Transactional(readOnly = true)
    public NotificationDto.SummaryResponse getNotificationSummary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        
        List<Notification> latestNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(1)).getContent();
        
        NotificationDto.Response latestNotification = latestNotifications.isEmpty() ? 
                null : NotificationDto.Response.from(latestNotifications.get(0));
        
        return NotificationDto.SummaryResponse.builder()
                .unreadCount(unreadCount)
                .latestNotification(latestNotification)
                .build();
    }
    
    /**
     * 알림 읽음 처리
     */
    @Transactional
    public NotificationDto.Response markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        
        notification.markAsRead();
        notification = notificationRepository.save(notification);
        
        return NotificationDto.Response.from(notification);
    }
    
    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
                
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }
    
    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
        
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * 오늘 발송된 알림 수 조회
     */
    public long getTodayNotificationCount(Long userId) {
        return notificationRepository.countTodayNotificationsByUserId(userId);
    }
    
    /**
     * 가격 알림 전송
     */
    @Transactional
    public void sendPriceAlert(Long userId, String coinSymbol, String alertMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Notification notification = Notification.builder()
                .user(user)
                .title("가격 알림")
                .message(alertMessage)
                .type("PRICE_ALERT")
                .resourceId(null)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        // 실시간 알림 전송 (WebSocket)
        sendRealtimeNotification(notification);
        
        // 푸시 알림 전송 (FCM 등)
        sendPushNotification(notification);
        
        log.info("가격 알림 생성 및 전송 완료: userId={}, coinSymbol={}", 
                user.getId(), coinSymbol);
    }
    
    /**
     * 실시간 WebSocket 알림 전송
     */
    private void sendRealtimeNotification(Notification notification) {
        try {
            NotificationDto.Response notificationDto = convertToDto(notification);
            
            // 개별 사용자에게 WebSocket 메시지 전송
            String destination = "/topic/notifications/" + notification.getUser().getId();
            messagingTemplate.convertAndSend(destination, notificationDto);
            
            // 실시간 알림 서비스를 통한 추가 처리
            realtimeNotificationService.sendToUser(notification.getUser().getId(), notificationDto);
            
            log.debug("실시간 알림 전송 완료: userId={}, destination={}", 
                    notification.getUser().getId(), destination);
                    
        } catch (Exception e) {
            log.error("실시간 알림 전송 실패: userId={}, error={}", 
                    notification.getUser().getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 푸시 알림 전송 (FCM)
     */
    private void sendPushNotification(Notification notification) {
        // 비동기로 푸시 알림 전송
        CompletableFuture.runAsync(() -> {
            try {
                // FCM 토큰이 있는 경우에만 푸시 알림 전송
                User user = notification.getUser();
                if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                    
                    // 실제 FCM 전송 로직
                    realtimeNotificationService.sendPushNotification(
                        user.getFcmToken(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getType()
                    );
                    
                    log.debug("푸시 알림 전송 완료: userId={}, fcmToken={}", 
                            user.getId(), user.getFcmToken().substring(0, 10) + "...");
                } else {
                    log.debug("FCM 토큰이 없어 푸시 알림 스킵: userId={}", user.getId());
                }
                
            } catch (Exception e) {
                log.error("푸시 알림 전송 실패: userId={}, error={}", 
                        notification.getUser().getId(), e.getMessage(), e);
            }
        });
    }

    /**
     * STOMP를 통한 개별 사용자 실시간 메시지 전송
     */
    public void sendToUserViaStomp(Long userId, Object message) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("STOMP 개별 사용자 메시지 전송 완료: userId={}, destination={}", userId, destination);
            
        } catch (Exception e) {
            log.error("STOMP 개별 사용자 메시지 전송 실패: userId={}", userId, e);
        }
    }

    /**
     * STOMP를 통한 브로드캐스트 메시지 전송
     */
    public void sendBroadcastViaStomp(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("STOMP 브로드캐스트 메시지 전송 완료: destination={}", destination);
            
        } catch (Exception e) {
            log.error("STOMP 브로드캐스트 메시지 전송 실패: destination={}", destination, e);
        }
    }

    /**
     * 사용자별 FCM 토큰 업데이트
     */
    @Transactional
    public void updateUserFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.updateFcmToken(fcmToken);
        userRepository.save(user);
        
        log.info("FCM 토큰 업데이트 완료: userId={}", userId);
    }



    /**
     * 특정 사용자에게 알림 전송 (실시간 알림 서비스에서 사용)
     */
    public void sendToUser(Long userId, NotificationDto.CreateRequest request) {
        createNotification(request);
    }

    /**
     * NotificationDto.Response를 내부 변환용으로 사용
     */
    private NotificationDto.Response convertToDto(Notification notification) {
        return NotificationDto.Response.from(notification);
    }
}
