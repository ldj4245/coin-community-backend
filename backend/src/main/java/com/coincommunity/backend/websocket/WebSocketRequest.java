package com.coincommunity.backend.websocket;

import lombok.Getter;
import lombok.Setter;

/**
 * 클라이언트로부터 받은 WebSocket 요청을 위한 DTO
 */
@Getter
@Setter
public class WebSocketRequest {
    
    /**
     * 요청 타입 (예: subscribe, unsubscribe)
     */
    private String type;
    
    /**
     * 구독하려는 코인 ID
     */
    private String coinId;
    
    /**
     * 추가 데이터 (필요에 따라 사용)
     */
    private Object data;
}
