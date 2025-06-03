package com.coincommunity.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 웹소켓 통신에 사용되는 DTO 클래스 모음
 */
public class WebSocketDto {

    /**
     * 웹소켓 클라이언트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSocketRequest {
        private String type;
        private String action;
        private Map<String, Object> data;
    }
    
    /**
     * 웹소켓 서버 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSocketResponse<T> {
        private String type;
        private boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        
        /**
         * 성공 응답 생성
         */
        public static <T> WebSocketResponse<T> success(String type, T data) {
            return WebSocketResponse.<T>builder()
                    .type(type)
                    .success(true)
                    .message("성공")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        /**
         * 실패 응답 생성
         */
        public static <T> WebSocketResponse<T> error(String type, String message) {
            return WebSocketResponse.<T>builder()
                    .type(type)
                    .success(false)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 코인 가격 정보 갱신 메시지
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoinPriceUpdate {
        private String coinId;
        private String name;
        private String symbol;
        private String exchange;
        private double price;
        private double change24h;
        private double volume24h;
        private double marketCap;
        private LocalDateTime lastUpdated;
    }
    
    /**
     * 사용자 알림 메시지
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
        private Long id;
        private Long userId;
        private String type;
        private String title;
        private String message;
        private boolean read;
        private Long relatedId; // 관련된 리소스 ID (게시글 ID, 댓글 ID 등)
        private String relatedType; // 관련된 리소스 타입 (post, comment 등)
        private LocalDateTime createdAt;
    }
    
    /**
     * 채팅 메시지
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String roomId;
        private Long senderId;
        private String senderName;
        private String senderProfileImage;
        private String content;
        private LocalDateTime timestamp;
    }
}
