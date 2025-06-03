package com.coincommunity.backend.config;

import com.coincommunity.backend.websocket.CoinPriceWebSocketHandler;
import com.coincommunity.backend.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket 설정을 위한 구성 클래스
 * STOMP 메시징 브로커와 기본 WebSocket 핸들러를 모두 지원
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final CoinPriceWebSocketHandler coinPriceWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    
    @Value("${websocket.endpoints.coin-prices:/ws/coin-prices}")
    private String coinPricesEndpoint;
    
    @Value("${websocket.endpoints.notifications:/ws/notifications}")
    private String notificationsEndpoint;
    
    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private String[] allowedOrigins;
    
    /**
     * 기본 WebSocket 핸들러 등록 (기존 구현 유지)
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 코인 가격 업데이트용 WebSocket
        registry.addHandler(coinPriceWebSocketHandler, coinPricesEndpoint)
                .setAllowedOrigins(allowedOrigins);
                
        // 알림용 WebSocket
        registry.addHandler(notificationWebSocketHandler, notificationsEndpoint)
                .setAllowedOrigins(allowedOrigins);
    }
    
    /**
     * STOMP 메시지 브로커 설정
     * SimpMessagingTemplate 빈을 제공하기 위한 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트에서 구독할 수 있는 토픽 prefix 설정
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 서버로 메시지를 전송할 때 사용할 prefix 설정
        config.setApplicationDestinationPrefixes("/app");
        
        // 특정 사용자에게 메시지를 전송할 때 사용할 prefix 설정
        config.setUserDestinationPrefix("/user");
    }
    
    /**
     * STOMP 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 연결을 위한 엔드포인트 설정
        registry.addEndpoint("/ws/stomp")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // SockJS 폴백 옵션 지원
    }
}
