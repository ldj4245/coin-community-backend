package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 관련 DTO 클래스
 */
public class NotificationDto {
    
    /**
     * 알림 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 응답 DTO")
    public static class Response {
        
        @Schema(description = "알림 ID")
        private Long id;
        
        @Schema(description = "알림 제목")
        private String title;
        
        @Schema(description = "알림 메시지")
        private String message;
        
        @Schema(description = "알림 타입(comment, like, follow, news, price-alert 등)")
        private String type;
        
        @Schema(description = "관련 리소스 ID")
        private Long resourceId;
        
        @Schema(description = "읽음 여부")
        private boolean isRead;
        
        @Schema(description = "알림 생성 시간")
        private LocalDateTime createdAt;
        
        @Schema(description = "알림 읽은 시간")
        private LocalDateTime readAt;
        
        /**
         * 엔티티를 DTO로 변환
         */
        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .type(notification.getType())
                    .resourceId(notification.getResourceId())
                    .isRead(notification.isRead())
                    .createdAt(notification.getCreatedAt())
                    .readAt(notification.getReadAt())
                    .build();
        }
        
        /**
         * 알림 엔티티 리스트를 DTO 리스트로 변환
         */
        public static List<Response> from(List<Notification> notifications) {
            return notifications.stream()
                    .map(Response::from)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 알림 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 생성 요청 DTO")
    public static class CreateRequest {
        
        @Schema(description = "알림을 받는 사용자 ID")
        private Long userId;
        
        @Schema(description = "알림 제목")
        private String title;
        
        @Schema(description = "알림 메시지")
        private String message;
        
        @Schema(description = "알림 타입(comment, like, follow, news, price-alert 등)")
        private String type;
        
        @Schema(description = "관련 리소스 ID")
        private Long resourceId;
    }
    
    /**
     * 알림 상태 요약 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 상태 요약 DTO")
    public static class SummaryResponse {
        
        @Schema(description = "읽지 않은 알림 개수")
        private long unreadCount;
        
        @Schema(description = "가장 최근 알림")
        private Response latestNotification;
    }
}
