package com.coincommunity.backend.websocket;

import com.coincommunity.backend.dto.CoinPriceDto;
import com.coincommunity.backend.entity.CoinPrice;
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
 * 코인 가격 실시간 업데이트를 위한 WebSocket 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinPriceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("새로운 WebSocket 연결이 설정되었습니다: {}", session.getId());
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결이 종료되었습니다: {}", session.getId());
        sessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("클라이언트로부터 메시지를 받았습니다: {}", payload);
            
            // 클라이언트로부터 받은 메시지 처리 (예: 구독 요청)
            WebSocketRequest request = objectMapper.readValue(payload, WebSocketRequest.class);
            
            // 요청 처리 로직 구현
            if ("subscribe".equals(request.getType())) {
                log.info("사용자 {}가 코인 데이터를 구독했습니다: {}", session.getId(), request.getCoinId());
                // 구독 처리 로직은 여기에 구현 (옵션)
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 모든 연결된 클라이언트에게 코인 가격 업데이트를 전송합니다.
     */
    public void broadcastPriceUpdate(CoinPriceDto.RealtimeUpdate update) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(update);
            TextMessage message = new TextMessage(jsonMessage);
            
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("메시지 전송 중 오류가 발생했습니다: {}", session.getId(), e);
                }
            });
            
            log.debug("코인 가격 업데이트를 {} 세션에 브로드캐스트했습니다", sessions.size());
        } catch (Exception e) {
            log.error("브로드캐스트 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 코인의 가격 업데이트를 전송합니다.
     */
    public void sendCoinPriceUpdate(CoinPrice coinPrice) {
        try {
            CoinPriceDto.RealtimeUpdate update = new CoinPriceDto.RealtimeUpdate(
                coinPrice.getCoinId(),
                coinPrice.getKoreanName(),
                coinPrice.getEnglishName(),
                coinPrice.getCurrentPrice(),
                coinPrice.getPriceChangePercent(),
                coinPrice.getExchange(),
                coinPrice.getLastUpdated()
            );
            
            broadcastPriceUpdate(update);
        } catch (Exception e) {
            log.error("코인 가격 업데이트 전송 중 오류가 발생했습니다", e);
        }
    }
}
