package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.service.SimpleCoinPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 코인 가격 정보 관련 API 엔드포인트 (단순화)
 */
@RestController
@RequestMapping("/api/v1/coins")
@RequiredArgsConstructor
@Tag(name = "코인 가격", description = "기본 암호화폐 가격 정보 조회 API")
public class CoinPriceController {

    private final SimpleCoinPriceService coinPriceService;

    /**
     * 주요 코인 가격 정보 조회
     */
    @Operation(
        summary = "주요 코인 가격 조회",
        description = "주요 암호화폐(BTC, ETH 등)의 현재 가격을 조회합니다."
    )
    @GetMapping("/major")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getMajorCoinPrices() {
        Map<String, BigDecimal> prices = coinPriceService.getMajorCoinPrices();
        return ResponseEntity.ok(ApiResponse.success(prices));
    }

    /**
     * 특정 코인의 가격 정보 조회
     */
    @Operation(
        summary = "특정 코인 가격 조회",
        description = "특정 코인의 현재 가격을 조회합니다."
    )
    @GetMapping("/{coinId}/price")
    public ResponseEntity<ApiResponse<BigDecimal>> getCoinPrice(
            @Parameter(description = "코인 ID (예: bitcoin, ethereum)") 
            @PathVariable String coinId) {
        
        BigDecimal price = coinPriceService.getCoinPrice(coinId);
        return ResponseEntity.ok(ApiResponse.success(price));
    }
}
