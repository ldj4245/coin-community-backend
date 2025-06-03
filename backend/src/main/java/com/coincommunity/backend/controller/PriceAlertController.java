package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PriceAlertDto;
import com.coincommunity.backend.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 가격 알림 관련 API 엔드포인트
 * 기본 경로: /api/price-alerts
 */
@RestController
@RequestMapping("/price-alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "가격 알림", description = "가격 알림 생성, 조회, 취소 관련 API")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    /**
     * 가격 알림 생성
     */
    @Operation(
        summary = "가격 알림 생성",
        description = "새로운 가격 알림을 생성합니다.",
        tags = {"가격 알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PriceAlertDto.Response>> createPriceAlert(
            @Valid @RequestBody PriceAlertDto.CreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        PriceAlertDto.Response response = priceAlertService.createPriceAlert(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 가격 알림 상세 조회
     */
    @Operation(
        summary = "가격 알림 상세 조회",
        description = "특정 가격 알림의 상세 정보를 조회합니다.",
        tags = {"가격 알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PriceAlertDto.Response>> getPriceAlert(
            @Parameter(description = "가격 알림 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        PriceAlertDto.Response response = priceAlertService.getPriceAlert(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자별 가격 알림 목록 조회
     */
    @Operation(
        summary = "사용자별 가격 알림 목록 조회",
        description = "현재 로그인한 사용자의 가격 알림 목록을 조회합니다.",
        tags = {"가격 알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PriceAlertDto.Response>>> getUserPriceAlerts(
            @Parameter(description = "페이지 정보 (size, page, sort)") 
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<PriceAlertDto.Response> page = priceAlertService.getUserPriceAlerts(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * 가격 알림 취소
     */
    @Operation(
        summary = "가격 알림 취소",
        description = "특정 가격 알림을 취소합니다.",
        tags = {"가격 알림"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelPriceAlert(
            @Parameter(description = "가격 알림 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        priceAlertService.cancelPriceAlert(id, userId);
        
        return ResponseEntity.ok(ApiResponse.successMessage("가격 알림이 취소되었습니다."));
    }
}