package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.dto.PriceAlertDto;
import com.coincommunity.backend.entity.PriceAlert;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.repository.PriceAlertRepository;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.websocket.Notification;
import com.coincommunity.backend.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 가격 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;
    private final ExchangePriceService exchangePriceService;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    // 최근 처리된 알림 캐시 (중복 알림 방지)
    private final Map<Long, LocalDateTime> recentlyProcessedAlerts = new ConcurrentHashMap<>();

    /**
     * 가격 알림 생성
     */
    @Transactional
    public PriceAlertDto.Response createPriceAlert(PriceAlertDto.CreateRequest request, Long userId) {
        log.info("가격 알림 생성 시작 - 사용자: {}, 코인: {}, 타입: {}, 가격: {}", 
                userId, request.getSymbol(), request.getAlertType(), request.getTargetPrice());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 현재 가격 조회
        List<ExchangePriceDto> prices = exchangePriceService.getExchangePrices(request.getSymbol());
        ExchangePriceDto priceInfo = prices.isEmpty() ? null : prices.get(0);

        PriceAlert priceAlert = PriceAlert.builder()
                .user(user)
                .symbol(request.getSymbol())
                .alertType(request.getAlertType())
                .targetPrice(request.getTargetPrice())
                .currentPrice(priceInfo != null ? priceInfo.getCurrentPrice() : null)
                .message(request.getMessage())
                .status(PriceAlertDto.AlertStatus.PENDING)
                .repeat(request.isRepeat())
                .build();

        PriceAlert savedAlert = priceAlertRepository.save(priceAlert);

        log.info("가격 알림 생성 완료 - ID: {}", savedAlert.getId());

        return convertToDto(savedAlert);
    }

    /**
     * 가격 알림 조회
     */
    @Transactional(readOnly = true)
    public PriceAlertDto.Response getPriceAlert(Long alertId, Long userId) {
        log.info("가격 알림 조회 - ID: {}, 사용자: {}", alertId, userId);

        PriceAlert priceAlert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("가격 알림을 찾을 수 없습니다: " + alertId));

        // 권한 확인
        if (!priceAlert.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 가격 알림에 접근할 권한이 없습니다.");
        }

        return convertToDto(priceAlert);
    }

    /**
     * 사용자별 가격 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PriceAlertDto.Response> getUserPriceAlerts(Long userId, Pageable pageable) {
        log.info("사용자별 가격 알림 목록 조회 - 사용자: {}", userId);

        Page<PriceAlert> alerts = priceAlertRepository.findByUserId(userId, pageable);

        return alerts.map(this::convertToDto);
    }

    /**
     * 가격 알림 취소
     */
    @Transactional
    public void cancelPriceAlert(Long alertId, Long userId) {
        log.info("가격 알림 취소 - ID: {}, 사용자: {}", alertId, userId);

        PriceAlert priceAlert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("가격 알림을 찾을 수 없습니다: " + alertId));

        // 권한 확인
        if (!priceAlert.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 가격 알림을 취소할 권한이 없습니다.");
        }

        priceAlert.updateStatus(PriceAlertDto.AlertStatus.CANCELLED);
        priceAlertRepository.save(priceAlert);

        log.info("가격 알림 취소 완료 - ID: {}", alertId);
    }

    /**
     * 가격 알림 처리 (스케줄러에서 호출)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void processPriceAlerts() {
        log.info("가격 알림 처리 시작");

        try {
            // 대기 중인 알림 조회
            List<PriceAlert> pendingAlerts = priceAlertRepository.findByStatus(PriceAlertDto.AlertStatus.PENDING);

            if (pendingAlerts.isEmpty()) {
                log.info("처리할 가격 알림이 없습니다.");
                return;
            }

            log.info("처리할 가격 알림 수: {}", pendingAlerts.size());

            // 코인별로 그룹화
            Map<String, List<PriceAlert>> alertsBySymbol = pendingAlerts.stream()
                    .collect(Collectors.groupingBy(PriceAlert::getSymbol));

            // 각 코인별로 처리
            for (Map.Entry<String, List<PriceAlert>> entry : alertsBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<PriceAlert> alerts = entry.getValue();

                // 현재 가격 조회
                List<ExchangePriceDto> prices = exchangePriceService.getExchangePrices(symbol);
                if (prices.isEmpty()) {
                    log.warn("코인 가격 정보를 찾을 수 없습니다: {}", symbol);
                    continue;
                }

                ExchangePriceDto priceInfo = prices.get(0);

                // 각 알림 처리 - 개별 트랜잭션으로 처리
                for (PriceAlert alert : alerts) {
                    try {
                        processAlertWithTransaction(alert, priceInfo);
                    } catch (Exception e) {
                        log.error("개별 알림 처리 중 오류 발생 - alertId: {}, symbol: {}", alert.getId(), alert.getSymbol(), e);
                        // 개별 알림 오류는 다른 알림 처리에 영향을 주지 않도록 함
                    }
                }
            }

            log.info("가격 알림 처리 완료");
        } catch (Exception e) {
            log.error("가격 알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 개별 알림 처리 (트랜잭션 분리)
     */
    @Transactional
    public void processAlertWithTransaction(PriceAlert alert, ExchangePriceDto priceInfo) {
        // 최근에 처리된 알림인지 확인 (5분 이내)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentlyProcessed = recentlyProcessedAlerts.get(alert.getId());
        if (recentlyProcessed != null && recentlyProcessed.plusMinutes(5).isAfter(now)) {
            return;
        }

        // 현재 가격 업데이트
        alert.updateCurrentPrice(priceInfo.getCurrentPrice());

        // 알림 조건 확인
        if (alert.checkAlertCondition()) {
            // 알림 상태 업데이트
            alert.updateStatus(PriceAlertDto.AlertStatus.TRIGGERED);
            priceAlertRepository.save(alert);

            // 알림 메시지 생성
            String message = alert.getMessage();
            if (message == null || message.isEmpty()) {
                message = String.format("%s 가격이 %s원에 %s 조건을 충족했습니다.", 
                        alert.getSymbol(), 
                        priceInfo.getCurrentPrice(), 
                        alert.getAlertType().getDisplayName());
            }

            // 웹소켓으로 알림 전송
            sendAlert(alert.getUser().getId(), message, alert);

            // 최근 처리된 알림으로 기록
            recentlyProcessedAlerts.put(alert.getId(), now);

            log.info("가격 알림 트리거됨 - ID: {}, 사용자: {}, 코인: {}, 타입: {}, 가격: {}",
                    alert.getId(), alert.getUser().getId(), alert.getSymbol(),
                    alert.getAlertType(), priceInfo.getCurrentPrice());

            // 반복 알림이 아니면 완료 상태로 변경
            if (!alert.isRepeat()) {
                alert.updateStatus(PriceAlertDto.AlertStatus.COMPLETED);
                priceAlertRepository.save(alert);
            } else {
                // 반복 알림은 다시 PENDING 상태로
                alert.updateStatus(PriceAlertDto.AlertStatus.PENDING);
                priceAlertRepository.save(alert);
            }
        }
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private PriceAlertDto.Response convertToDto(PriceAlert priceAlert) {
        return PriceAlertDto.Response.builder()
                .id(priceAlert.getId())
                .userId(priceAlert.getUser().getId())
                .symbol(priceAlert.getSymbol())
                .alertType(priceAlert.getAlertType())
                .targetPrice(priceAlert.getTargetPrice())
                .currentPrice(priceAlert.getCurrentPrice())
                .message(priceAlert.getMessage())
                .status(priceAlert.getStatus())
                .repeat(priceAlert.isRepeat())
                .createdAt(priceAlert.getCreatedAt())
                .lastTriggeredAt(priceAlert.getLastTriggeredAt())
                .build();
    }

    /**
     * 웹소켓을 통해 알림을 전송합니다.
     *
     * @param userId 알림을 받을 사용자 ID
     * @param message 알림 메시지
     * @param alert 관련 가격 알림 엔티티
     */
    private void sendAlert(Long userId, String message, PriceAlert alert) {
        try {
            Notification notification = Notification.builder()
                .type("PRICE_ALERT")
                .message(message)
                .userId(userId)
                .data(Map.of(
                    "alertId", alert.getId(),
                    "symbol", alert.getSymbol(),
                    "targetPrice", alert.getTargetPrice(),
                    "currentPrice", alert.getCurrentPrice(),
                    "alertType", alert.getAlertType().name(),
                    "timestamp", LocalDateTime.now().toString() // timestamp를 data 맵에 추가
                ))
                .build();

            sendNotificationToUser(userId, notification);
            log.info("가격 알림 웹소켓 전송 완료 - 사용자: {}, 코인: {}", userId, alert.getSymbol());
        } catch (Exception e) {
            // 알림 전송 실패가 알림 처리 전체를 실패시키지 않도록 예외 처리
            log.error("가격 알림 웹소켓 전송 실패 - 사용자: {}, 코인: {}", userId, alert.getSymbol(), e);
        }
    }

    /**
     * 웹소켓을 통해 사용자에게 알림을 전송합니다.
     */
    private void sendNotificationToUser(Long userId, Notification notification) {
        try {
            notificationWebSocketHandler.sendNotification(userId, notification);
        } catch (Exception e) {
            log.error("알림 전송 실패: {}", e.getMessage());
        }
    }
}
