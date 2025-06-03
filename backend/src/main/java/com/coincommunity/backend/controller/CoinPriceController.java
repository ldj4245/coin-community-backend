package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.CoinPriceDto;
import com.coincommunity.backend.service.CoinPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 코인 가격 정보 관련 API 엔드포인트
 * 기본 경로: /api/coins
 */
@RestController
@RequestMapping("/coins")
@RequiredArgsConstructor
@Tag(name = "코인 가격", description = "암호화폐 가격 정보 조회 API")
public class CoinPriceController {

    private final CoinPriceService coinPriceService;

    /**
     * 모든 코인 가격 정보 조회
     */
    @Operation(
        summary = "모든 코인 가격 정보 조회",
        description = "현재 지원하는 모든 코인들의 가격 정보를 제공합니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CoinPriceDto.CoinPriceResponse>>> getAllCoinPrices() {
        List<CoinPriceDto.CoinPriceResponse> coinPrices = coinPriceService.getAllCoinPrices();
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(coinPrices));
    }

    /**
     * 특정 코인의 가격 정보 조회
     */
    @Operation(
        summary = "특정 코인의 가격 정보 조회",
        description = "특정 코인의 가격 정보를 조회합니다. 거래소는 기본적으로 UPBIT를 사용합니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/{coinId}")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<CoinPriceDto.CoinPriceResponse>> getCoinPrice(
            @Parameter(description = "코인 ID(심볼)") @PathVariable String coinId,
            @Parameter(description = "거래소명 (기본값: UPBIT)") @RequestParam(required = false, defaultValue = "UPBIT") String exchange) {
        
        CoinPriceDto.CoinPriceResponse coinPrice = 
                coinPriceService.getCoinPriceByExchange(coinId, exchange);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(coinPrice));
    }

    /**
     * 특정 거래소의 모든 코인 가격 정보 조회
     */
    @Operation(
        summary = "특정 거래소의 모든 코인 가격 정보 조회",
        description = "특정 거래소(예: UPBIT, BINANCE)에서 처리하는 모든 코인의 가격 정보를 조회합니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/exchange/{exchange}")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CoinPriceDto.CoinPriceResponse>>> getCoinPricesByExchange(
            @Parameter(description = "거래소명 (UPBIT, BINANCE 등)") @PathVariable String exchange) {
        
        List<CoinPriceDto.CoinPriceResponse> coinPrices = 
                coinPriceService.getCoinPricesByExchange(exchange);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(coinPrices));
    }

    /**
     * 시가총액 기준 상위 코인 목록 조회
     */
    @Operation(
        summary = "시가총액 기준 상위 코인 목록",
        description = "시가총액이 크니 순으로 코인 목록을 가져옵니다. 개수를 제한할 수 있습니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/top-market-cap")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CoinPriceDto.CoinPriceResponse>>> getTopCoinsByMarketCap(
            @Parameter(description = "가져올 코인 개수 (기본값: 10)") @RequestParam(defaultValue = "10") int limit) {
        
        List<CoinPriceDto.CoinPriceResponse> topCoins = 
                coinPriceService.getTopCoinsByMarketCap(limit);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(topCoins));
    }

    /**
     * 상승률 기준 상위 코인 목록 조회
     */
    @Operation(
        summary = "상승률 기준 상위 코인 목록",
        description = "가격 상승률이 높은 순으로 코인 목록을 조회합니다. 24시간 동안 상승율이 높은 코인을 확인할 수 있습니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/top-gainers")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CoinPriceDto.CoinPriceResponse>>> getTopGainers(
            @Parameter(description = "가져올 코인 개수 (기본값: 10)") @RequestParam(defaultValue = "10") int limit) {
        
        List<CoinPriceDto.CoinPriceResponse> topGainers = 
                coinPriceService.getTopGainers(limit);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(topGainers));
    }

    /**
     * 하락률 기준 상위 코인 목록 조회
     */
    @Operation(
        summary = "하락률 기준 상위 코인 목록",
        description = "가격 하락률이 높은 순으로 코인 목록을 조회합니다. 24시간 동안 하락율이 높은 코인을 확인할 수 있습니다.",
        tags = {"코인 가격"}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/top-losers")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CoinPriceDto.CoinPriceResponse>>> getTopLosers(
            @Parameter(description = "가져올 코인 개수 (기본값: 10)") @RequestParam(defaultValue = "10") int limit) {
        
        List<CoinPriceDto.CoinPriceResponse> topLosers = 
                coinPriceService.getTopLosers(limit);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(topLosers));
    }
}
