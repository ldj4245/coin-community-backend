package com.coincommunity.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 알림을 위한 WebSocket 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("새로운 알림 WebSocket 연결이 설정되었습니다: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("알림 WebSocket 연결이 종료되었습니다: {}", session.getId());
        Long userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("알림 WebSocket 메시지를 받았습니다: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("알림 WebSocket 전송 오류: ", exception);
    }

    /**
     * 특정 사용자에게 알림을 전송합니다.
     */
    public void sendNotificationToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("알림 전송 실패 (사용자 ID: {}): ", userId, e);
            }
        }
    }

    /**
     * 특정 사용자에게 구조화된 알림 객체를 전송합니다.
     */
    public void sendNotification(Long userId, Object notification) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);
            sendNotificationToUser(userId, jsonMessage);
            log.debug("구조화된 알림 전송 완료: 사용자ID={}", userId);
        } catch (Exception e) {
            log.error("구조화된 알림 전송 실패: 사용자ID={}", userId, e);
        }
    }

    /**
     * 모든 연결된 사용자에게 브로드캐스트 알림을 전송합니다.
     */
    public void broadcastNotification(Object notification) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);
            TextMessage message = new TextMessage(jsonMessage);
            
            int successCount = 0;
            int failCount = 0;
            
            for (WebSocketSession session : userSessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                        successCount++;
                    }
                } catch (IOException e) {
                    failCount++;
                    log.warn("브로드캐스트 개별 전송 실패: 세션ID={}", session.getId());
                }
            }
            
            log.info("브로드캐스트 알림 전송 완료: 성공={}건, 실패={}건, 총세션={}개", 
                    successCount, failCount, userSessions.size());
                    
        } catch (Exception e) {
            log.error("브로드캐스트 알림 처리 실패", e);
        }
    }

    /**
     * 사용자 세션 등록 (연결 시 사용자 ID 매핑)
     */
    public void registerUserSession(String sessionId, Long userId) {
        WebSocketSession session = findSessionById(sessionId);
        if (session != null) {
            userSessions.put(userId, session);
            sessionUserMap.put(sessionId, userId);
            log.info("사용자 세션 등록 완료: 사용자ID={}, 세션ID={}", userId, sessionId);
        }
    }

    /**
     * 세션 ID로 WebSocket 세션 찾기
     */
    private WebSocketSession findSessionById(String sessionId) {
        return userSessions.values().stream()
                .filter(session -> sessionId.equals(session.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 연결된 사용자 수 조회
     */
    public int getConnectedUserCount() {
        return userSessions.size();
    }

    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
