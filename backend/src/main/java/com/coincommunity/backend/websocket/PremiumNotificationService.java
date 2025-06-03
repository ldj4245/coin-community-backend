package com.coincommunity.backend.websocket;

import com.coincommunity.backend.dto.KimchiPremiumDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 김치프리미엄 실시간 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PremiumNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // 최근 알림 내역을 저장하기 위한 맵 (코인 -> 마지막 알림 시간)
    private final Map<String, LocalDateTime> lastNotifications = new HashMap<>();

    // 알림 간격 (분) - 같은 코인에 대해 반복 알림을 방지
    private static final int NOTIFICATION_INTERVAL_MINUTES = 30;

    // 중요 알림 기준 김치프리미엄 비율
    private static final BigDecimal HIGH_PREMIUM_THRESHOLD = new BigDecimal("5.0");
    private static final BigDecimal LOW_PREMIUM_THRESHOLD = new BigDecimal("-1.0"); // 역프리미엄

    /**
     * 김치프리미엄 변동 알림 전송
     */
    public void notifyPremiumChange(KimchiPremiumDto premiumDto) {
        if (premiumDto == null || premiumDto.getPremiumRate() == null) {
            return;
        }

        String symbol = premiumDto.getSymbol();
        BigDecimal premiumRate = premiumDto.getPremiumRate();

        // 중요 알림 기준에 해당하는지 확인
        boolean isHighPremium = premiumRate.compareTo(HIGH_PREMIUM_THRESHOLD) >= 0;
        boolean isLowPremium = premiumRate.compareTo(LOW_PREMIUM_THRESHOLD) <= 0;

        if (!isHighPremium && !isLowPremium) {
            return; // 중요 알림 기준에 해당하지 않으면 알림 전송 안함
        }

        // 최근 알림 시간 확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastNotification = lastNotifications.get(symbol);

        if (lastNotification != null && 
            lastNotification.plusMinutes(NOTIFICATION_INTERVAL_MINUTES).isAfter(now)) {
            // 알림 간격이 지나지 않았으면 알림 전송 안함
            return;
        }

        // 알림 데이터 구성
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("symbol", symbol);
        notificationData.put("koreanName", premiumDto.getKoreanName());
        notificationData.put("premiumRate", premiumRate);
        notificationData.put("premiumAmount", premiumDto.getPremiumAmount());
        notificationData.put("highestExchange", premiumDto.getHighestDomesticExchange());
        notificationData.put("baseExchange", premiumDto.getBaseExchange());

        // 알림 메시지 구성
        String title = isHighPremium ? "높은 김치프리미엄 감지" : "역프리미엄 감지";
        String message = String.format("%s(%s) %s: %.2f%% (₩%,.0f)",
                                    premiumDto.getKoreanName(),
                                    symbol, 
                                    isHighPremium ? "김치프리미엄" : "역프리미엄",
                                    premiumRate,
                                    premiumDto.getPremiumAmount());

        // 알림 객체 생성
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type("premium-alert")
                .data(notificationData)
                .createdAt(now)
                .build();

        // 웹소켓으로 알림 전송
        try {
            messagingTemplate.convertAndSend("/topic/premium-alerts", notification);
            log.info("김치프리미엄 알림 전송 성공: {}", message);

            // 마지막 알림 시간 업데이트
            lastNotifications.put(symbol, now);
        } catch (Exception e) {
            log.error("김치프리미엄 알림 전송 실패", e);
        }
    }
}
