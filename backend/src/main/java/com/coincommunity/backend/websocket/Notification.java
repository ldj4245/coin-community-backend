package com.coincommunity.backend.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WebSocket을 통해 클라이언트에게 전송되는 알림 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    /**
     * 알림 대상 사용자 ID (전체 사용자 대상 알림인 경우 null)
     */
    private Long userId;
    
    /**
     * 알림 메시지
     */
    private String message;
    
    /**
     * 알림 타입 (예: comment, like, follow, news, price-alert)
     */
    private String type;
    
    /**
     * 알림 생성 시간
     */
    private LocalDateTime timestamp;
}
