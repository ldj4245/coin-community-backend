package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.TrendingCoinDto;
import com.coincommunity.backend.service.TrendingCoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 트렌딩 코인 관련 API 엔드포인트
 * 기본 경로: /api/trending-coins
 */
@RestController
@RequestMapping("/trending-coins")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "트렌딩 코인", description = "트렌딩 코인 조회 관련 API")
public class TrendingCoinController {

    private final TrendingCoinService trendingCoinService;

    /**
     * 트렌딩 코인 목록 조회
     */
    @Operation(
        summary = "트렌딩 코인 목록 조회",
        description = "가격 변동, 거래량, 커뮤니티 언급 등을 기반으로 한 트렌딩 코인 목록을 조회합니다.",
        tags = {"트렌딩 코인"}
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrendingCoinDto>>> getTrendingCoins(
            @Parameter(description = "조회할 코인 수") 
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "정렬 기준 (PRICE_CHANGE, VOLUME, MARKET_CAP, COMMUNITY_MENTIONS, TRENDING_SCORE)") 
            @RequestParam(defaultValue = "TRENDING_SCORE") TrendingCoinDto.SortBy sortBy) {
        
        log.info("트렌딩 코인 목록 조회 요청 - 개수: {}, 정렬: {}", limit, sortBy);
        
        List<TrendingCoinDto> trendingCoins = trendingCoinService.getTrendingCoins(limit, sortBy);
        
        return ResponseEntity.ok(ApiResponse.success(trendingCoins));
    }

    /**
     * 가격 변동이 큰 코인 목록 조회
     */
    @Operation(
        summary = "가격 변동이 큰 코인 목록 조회",
        description = "가격 변동률이 큰 코인 목록을 조회합니다.",
        tags = {"트렌딩 코인"}
    )
    @GetMapping("/price-movers")
    public ResponseEntity<ApiResponse<List<TrendingCoinDto>>> getPriceMovers(
            @Parameter(description = "조회할 코인 수") 
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("가격 변동이 큰 코인 목록 조회 요청 - 개수: {}", limit);
        
        List<TrendingCoinDto> priceMovers = trendingCoinService.getTrendingCoins(limit, TrendingCoinDto.SortBy.PRICE_CHANGE);
        
        return ResponseEntity.ok(ApiResponse.success(priceMovers));
    }

    /**
     * 커뮤니티에서 많이 언급된 코인 목록 조회
     */
    @Operation(
        summary = "커뮤니티에서 많이 언급된 코인 목록 조회",
        description = "커뮤니티 게시글에서 많이 언급된 코인 목록을 조회합니다.",
        tags = {"트렌딩 코인"}
    )
    @GetMapping("/community-mentions")
    public ResponseEntity<ApiResponse<List<TrendingCoinDto>>> getCommunityMentions(
            @Parameter(description = "조회할 코인 수") 
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("커뮤니티에서 많이 언급된 코인 목록 조회 요청 - 개수: {}", limit);
        
        List<TrendingCoinDto> communityMentions = trendingCoinService.getTrendingCoins(limit, TrendingCoinDto.SortBy.COMMUNITY_MENTIONS);
        
        return ResponseEntity.ok(ApiResponse.success(communityMentions));
    }

    /**
     * 거래량이 많은 코인 목록 조회
     */
    @Operation(
        summary = "거래량이 많은 코인 목록 조회",
        description = "24시간 거래량이 많은 코인 목록을 조회합니다.",
        tags = {"트렌딩 코인"}
    )
    @GetMapping("/high-volume")
    public ResponseEntity<ApiResponse<List<TrendingCoinDto>>> getHighVolumeCoins(
            @Parameter(description = "조회할 코인 수") 
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("거래량이 많은 코인 목록 조회 요청 - 개수: {}", limit);
        
        List<TrendingCoinDto> highVolumeCoins = trendingCoinService.getTrendingCoins(limit, TrendingCoinDto.SortBy.VOLUME);
        
        return ResponseEntity.ok(ApiResponse.success(highVolumeCoins));
    }
}