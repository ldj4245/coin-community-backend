package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.CoinWatchlistDto;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.service.CoinWatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 관심종목 관리 컨트롤러
 * 사용자의 관심 암호화폐 관리 및 가격 알림 기능을 제공합니다.
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "CoinWatchlist", description = "관심종목 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class CoinWatchlistController {

    private final CoinWatchlistService coinWatchlistService;

    /**
     * 관심종목 추가
     */
    @PostMapping
    @Operation(summary = "관심종목 추가", description = "새로운 코인을 관심종목에 추가합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "관심종목 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 추가된 관심종목")
    })
    public ResponseEntity<ApiResponse<CoinWatchlistDto.Response>> addToWatchlist(
            @Valid @RequestBody CoinWatchlistDto.AddRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 추가 요청: 사용자={}, 코인={}", 
                userDetails.getUsername(), request.getCoinSymbol());
        
        CoinWatchlistDto.Response watchlistItem = coinWatchlistService.addToWatchlist(
                userDetails.getUsername(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("관심종목이 성공적으로 추가되었습니다.", watchlistItem));
    }

    /**
     * 내 관심종목 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 관심종목 목록", description = "로그인한 사용자의 관심종목 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<PageResponse<CoinWatchlistDto.Response>>> getMyWatchlist(
            @Parameter(description = "카테고리 필터 (선택사항)") 
            @RequestParam(required = false) String category,
            @Parameter(description = "가격 알림 활성화 필터 (선택사항)") 
            @RequestParam(required = false) Boolean priceAlertEnabled,
            @Parameter(description = "정렬 기준 (CREATED_AT_DESC, CHANGE_PERCENTAGE_DESC, etc.)") 
            @RequestParam(defaultValue = "CREATED_AT_DESC") String sortBy,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 관심종목 목록 조회: 사용자={}, 카테고리={}", 
                userDetails.getUsername(), category);
        
        CoinWatchlistDto.FilterRequest filter = CoinWatchlistDto.FilterRequest.builder()
                .category(category)
                .priceAlertEnabled(priceAlertEnabled)
                .sortBy(sortBy)
                .build();
        
        PageResponse<CoinWatchlistDto.Response> watchlist = coinWatchlistService.getUserWatchlist(
                userDetails.getUsername(), filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("관심종목 목록을 성공적으로 조회했습니다.", watchlist));
    }

    /**
     * 관심종목 상세 조회
     */
    @GetMapping("/{watchlistId}")
    @Operation(summary = "관심종목 상세 조회", description = "관심종목의 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinWatchlistDto.Response>> getWatchlistItem(
            @Parameter(description = "관심종목 ID") @PathVariable @Min(1) Long watchlistId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 상세 조회: ID={}, 사용자={}", watchlistId, userDetails.getUsername());
        
        CoinWatchlistDto.Response watchlistItem = coinWatchlistService.getWatchlistItem(
                watchlistId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("관심종목 정보를 성공적으로 조회했습니다.", watchlistItem));
    }

    /**
     * 관심종목 수정
     */
    @PutMapping("/{watchlistId}")
    @Operation(summary = "관심종목 수정", description = "관심종목 정보를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinWatchlistDto.Response>> updateWatchlistItem(
            @Parameter(description = "관심종목 ID") @PathVariable @Min(1) Long watchlistId,
            @Valid @RequestBody CoinWatchlistDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 수정: ID={}, 사용자={}", watchlistId, userDetails.getUsername());
        
        CoinWatchlistDto.Response watchlistItem = coinWatchlistService.updateWatchlistItem(
                watchlistId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("관심종목이 성공적으로 수정되었습니다.", watchlistItem));
    }

    /**
     * 관심종목 삭제
     */
    @DeleteMapping("/{watchlistId}")
    @Operation(summary = "관심종목 삭제", description = "관심종목에서 코인을 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(
            @Parameter(description = "관심종목 ID") @PathVariable @Min(1) Long watchlistId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 삭제: ID={}, 사용자={}", watchlistId, userDetails.getUsername());
        
        coinWatchlistService.removeFromWatchlist(watchlistId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successMessage("관심종목이 성공적으로 삭제되었습니다."));
    }

    /**
     * 코인 심볼로 관심종목 삭제
     */
    @DeleteMapping("/coin/{coinSymbol}")
    @Operation(summary = "코인 심볼로 관심종목 삭제", description = "코인 심볼을 통해 관심종목에서 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlistByCoin(
            @Parameter(description = "코인 심볼") @PathVariable String coinSymbol,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인 심볼로 관심종목 삭제: 코인={}, 사용자={}", coinSymbol, userDetails.getUsername());
        
        coinWatchlistService.removeFromWatchlistByCoin(userDetails.getUsername(), coinSymbol);
        
        return ResponseEntity.ok(ApiResponse.successMessage("관심종목이 성공적으로 삭제되었습니다."));
    }

    /**
     * 가격 알림 설정
     */
    @PutMapping("/{watchlistId}/price-alert")
    @Operation(summary = "가격 알림 설정", description = "관심종목의 가격 알림을 설정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가격 알림 설정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "설정 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinWatchlistDto.Response>> setPriceAlert(
            @Parameter(description = "관심종목 ID") @PathVariable @Min(1) Long watchlistId,
            @Valid @RequestBody CoinWatchlistDto.PriceAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("가격 알림 설정: ID={}, 사용자={}, 활성화={}", 
                watchlistId, userDetails.getUsername(), request.getPriceAlertEnabled());
        
        CoinWatchlistDto.Response watchlistItem = coinWatchlistService.setPriceAlert(
                watchlistId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("가격 알림이 성공적으로 설정되었습니다.", watchlistItem));
    }

    /**
     * 관심종목 통계 조회
     */
    @GetMapping("/my/statistics")
    @Operation(summary = "내 관심종목 통계", description = "로그인한 사용자의 관심종목 통계를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 통계 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinWatchlistDto.Statistics>> getMyWatchlistStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 관심종목 통계 조회: 사용자={}", userDetails.getUsername());
        
        CoinWatchlistDto.Statistics statistics = coinWatchlistService.getUserWatchlistStatistics(
                userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("관심종목 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    @Operation(summary = "카테고리 목록", description = "사용자의 관심종목 카테고리 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<String>>> getWatchlistCategories(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("카테고리 목록 조회: 사용자={}", userDetails.getUsername());
        
        List<String> categories = coinWatchlistService.getUserWatchlistCategories(
                userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("카테고리 목록을 성공적으로 조회했습니다.", categories));
    }

    /**
     * 가격 알림 트리거된 종목 조회
     */
    @GetMapping("/alerts/triggered")
    @Operation(summary = "가격 알림 트리거 종목", description = "가격 알림이 트리거된 관심종목을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가격 알림 트리거 종목 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinWatchlistDto.Response>>> getTriggeredAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("가격 알림 트리거 종목 조회: 사용자={}", userDetails.getUsername());
        
        List<CoinWatchlistDto.Response> triggeredAlerts = coinWatchlistService.getTriggeredPriceAlerts(
                userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("가격 알림 트리거 종목을 성공적으로 조회했습니다.", triggeredAlerts));
    }

    /**
     * 관심종목 가격 업데이트
     */
    @PostMapping("/refresh-prices")
    @Operation(summary = "관심종목 가격 갱신", description = "모든 관심종목의 가격을 최신으로 업데이트합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 가격 갱신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinWatchlistDto.Summary>>> refreshWatchlistPrices(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 가격 갱신: 사용자={}", userDetails.getUsername());
        
        List<CoinWatchlistDto.Summary> updatedItems = coinWatchlistService.refreshWatchlistPrices(
                userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("관심종목 가격이 성공적으로 갱신되었습니다.", updatedItems));
    }

    /**
     * 인기 관심종목 조회
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 관심종목", description = "다른 사용자들이 많이 추가한 인기 관심종목을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 관심종목 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinWatchlistDto.Summary>>> getPopularWatchlistCoins(
            @Parameter(description = "조회할 코인 수 (기본값: 10)") 
            @RequestParam(defaultValue = "10") @Min(1) Integer limit,
            @Parameter(description = "기간 (일수, 기본값: 7)") 
            @RequestParam(defaultValue = "7") @Min(1) Integer days) {
        
        log.info("인기 관심종목 조회: 제한={}, 기간={}일", limit, days);
        
        List<CoinWatchlistDto.Summary> popularCoins = coinWatchlistService.getPopularWatchlistCoins(
                limit, days);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("인기 관심종목을 성공적으로 조회했습니다.", popularCoins));
    }

    /**
     * 관심종목 일괄 추가
     */
    @PostMapping("/bulk")
    @Operation(summary = "관심종목 일괄 추가", description = "여러 코인을 한 번에 관심종목에 추가합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "관심종목 일괄 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 추가된 관심종목이 포함됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinWatchlistDto.Response>>> addBulkToWatchlist(
            @Parameter(description = "코인 심볼 목록") @RequestBody List<String> coinSymbols,
            @Parameter(description = "기본 카테고리 (선택사항)") 
            @RequestParam(required = false) String defaultCategory,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 일괄 추가: 사용자={}, 코인수={}", 
                userDetails.getUsername(), coinSymbols.size());
        
        List<CoinWatchlistDto.Response> addedItems = coinWatchlistService.addBulkToWatchlist(
                userDetails.getUsername(), coinSymbols, defaultCategory);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("관심종목이 성공적으로 일괄 추가되었습니다.", addedItems));
    }

    /**
     * 관심종목 일괄 삭제
     */
    @DeleteMapping("/bulk")
    @Operation(summary = "관심종목 일괄 삭제", description = "여러 관심종목을 한 번에 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심종목 일괄 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일부 관심종목을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeBulkFromWatchlist(
            @Parameter(description = "관심종목 ID 목록") @RequestBody List<Long> watchlistIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("관심종목 일괄 삭제: 사용자={}, 삭제수={}", 
                userDetails.getUsername(), watchlistIds.size());
        
        coinWatchlistService.removeBulkFromWatchlist(userDetails.getUsername(), watchlistIds);
        
        return ResponseEntity.ok(ApiResponse.successMessage("관심종목이 성공적으로 일괄 삭제되었습니다."));
    }
}
