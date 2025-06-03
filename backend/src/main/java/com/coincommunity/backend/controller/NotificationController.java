package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.NotificationDto;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 관련 API 엔드포인트
 * 기본 경로: /api/notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 조회, 읽음 처리 등 알림 관련 API")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 알림 목록 조회 (페이징)
     */
    @Operation(
        summary = "알림 목록 조회",
        description = "현재 로그인한 사용자의 알림 목록을 페이지네이션으로 조회합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto.Response>>> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 정보 (size, page, sort)") 
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
            
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<NotificationDto.Response> page = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
    
    /**
     * 읽지 않은 알림 목록 조회
     */
    @Operation(
        summary = "읽지 않은 알림 목록 조회",
        description = "현재 로그인한 사용자의 읽지 않은 알림 목록을 조회합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationDto.Response>>> getUnreadNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
            
        Long userId = Long.parseLong(userDetails.getUsername());
        List<NotificationDto.Response> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
    
    /**
     * 알림 상태 요약 조회
     */
    @Operation(
        summary = "알림 상태 요약 조회",
        description = "현재 로그인한 사용자의 읽지 않은 알림 개수와 최신 알림을 조회합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<NotificationDto.SummaryResponse>> getNotificationSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
            
        Long userId = Long.parseLong(userDetails.getUsername());
        NotificationDto.SummaryResponse summary = notificationService.getNotificationSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * 단일 알림 읽음 처리
     */
    @Operation(
        summary = "단일 알림 읽음 처리",
        description = "특정 알림을 읽음 상태로 변경합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto.Response>> markAsRead(
            @Parameter(description = "알림 ID") @PathVariable Long id) {
            
        NotificationDto.Response response = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @Operation(
        summary = "모든 알림 읽음 처리",
        description = "현재 로그인한 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
            
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("요청이 성공적으로 처리되었습니다."));
    }
    
    /**
     * 알림 삭제
     */
    @Operation(
        summary = "알림 삭제",
        description = "특정 알림을 삭제합니다.",
        tags = {"알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "알림 ID") @PathVariable Long id) {
            
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.successMessage("요청이 성공적으로 처리되었습니다."));
    }
}
