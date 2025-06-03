package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PortfolioDto;
import com.coincommunity.backend.dto.PortfolioItemDto;
import com.coincommunity.backend.service.PortfolioService;
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
 * 포트폴리오 관리 컨트롤러
 * 사용자의 암호화폐 포트폴리오 CRUD 및 분석 기능을 제공합니다.
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Portfolio", description = "포트폴리오 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * 포트폴리오 생성
     */
    @PostMapping
    @Operation(summary = "포트폴리오 생성", description = "새로운 포트폴리오를 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "포트폴리오 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<PortfolioDto.Response>> createPortfolio(
            @Valid @RequestBody PortfolioDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 생성 요청: 사용자={}, 이름={}", userDetails.getUsername(), request.getName());
        
        PortfolioDto.Response portfolio = portfolioService.createPortfolio(userDetails.getUsername(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("포트폴리오가 성공적으로 생성되었습니다.", portfolio));
    }

    /**
     * 내 포트폴리오 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 포트폴리오 목록 조회", description = "로그인한 사용자의 포트폴리오 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDto.Summary>>> getMyPortfolios(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 포트폴리오 목록 조회: 사용자={}", userDetails.getUsername());
        
        PageResponse<PortfolioDto.Summary> portfolios = portfolioService.getUserPortfolios(
                userDetails.getUsername(), pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 목록을 성공적으로 조회했습니다.", portfolios));
    }

    /**
     * 포트폴리오 상세 조회
     */
    @GetMapping("/{portfolioId}")
    @Operation(summary = "포트폴리오 상세 조회", description = "포트폴리오의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<PortfolioDto.Response>> getPortfolio(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 상세 조회: ID={}, 사용자={}", portfolioId, userDetails.getUsername());
        
        PortfolioDto.Response portfolio = portfolioService.getPortfolioDetails(
                portfolioId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 정보를 성공적으로 조회했습니다.", portfolio));
    }

    /**
     * 포트폴리오 수정
     */
    @PutMapping("/{portfolioId}")
    @Operation(summary = "포트폴리오 수정", description = "포트폴리오 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<PortfolioDto.Response>> updatePortfolio(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Valid @RequestBody PortfolioDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 수정: ID={}, 사용자={}", portfolioId, userDetails.getUsername());
        
        PortfolioDto.Response portfolio = portfolioService.updatePortfolio(
                portfolioId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오가 성공적으로 수정되었습니다.", portfolio));
    }

    /**
     * 포트폴리오 삭제
     */
    @DeleteMapping("/{portfolioId}")
    @Operation(summary = "포트폴리오 삭제", description = "포트폴리오를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 삭제: ID={}, 사용자={}", portfolioId, userDetails.getUsername());
        
        portfolioService.deletePortfolio(portfolioId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successMessage("포트폴리오가 성공적으로 삭제되었습니다."));
    }

    /**
     * 포트폴리오 아이템 추가
     */
    @PostMapping("/{portfolioId}/items")
    @Operation(summary = "포트폴리오 아이템 추가", description = "포트폴리오에 새로운 코인을 추가합니다.")
    public ResponseEntity<ApiResponse<PortfolioItemDto.Response>> addPortfolioItem(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Valid @RequestBody PortfolioItemDto.AddRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 아이템 추가: 포트폴리오ID={}, 코인={}, 사용자={}", 
                portfolioId, request.getCoinSymbol(), userDetails.getUsername());
        
        PortfolioItemDto.Response item = portfolioService.addPortfolioItem(
                portfolioId, userDetails.getUsername(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("포트폴리오 아이템이 성공적으로 추가되었습니다.", item));
    }

    /**
     * 포트폴리오 아이템 수정
     */
    @PutMapping("/{portfolioId}/items/{itemId}")
    @Operation(summary = "포트폴리오 아이템 수정", description = "포트폴리오 아이템 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<PortfolioItemDto.Response>> updatePortfolioItem(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Parameter(description = "아이템 ID") @PathVariable @Min(1) Long itemId,
            @Valid @RequestBody PortfolioItemDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 아이템 수정: 포트폴리오ID={}, 아이템ID={}, 사용자={}", 
                portfolioId, itemId, userDetails.getUsername());
        
        PortfolioItemDto.Response item = portfolioService.updatePortfolioItem(
                portfolioId, itemId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 아이템이 성공적으로 수정되었습니다.", item));
    }

    /**
     * 포트폴리오 아이템 삭제
     */
    @DeleteMapping("/{portfolioId}/items/{itemId}")
    @Operation(summary = "포트폴리오 아이템 삭제", description = "포트폴리오에서 아이템을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> removePortfolioItem(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Parameter(description = "아이템 ID") @PathVariable @Min(1) Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 아이템 삭제: 포트폴리오ID={}, 아이템ID={}, 사용자={}", 
                portfolioId, itemId, userDetails.getUsername());
        
        portfolioService.removePortfolioItem(portfolioId, itemId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successMessage("포트폴리오 아이템이 성공적으로 삭제되었습니다."));
    }

    /**
     * 내 포트폴리오 통계 조회
     */
    @GetMapping("/my/statistics")
    @Operation(summary = "내 포트폴리오 통계", description = "로그인한 사용자의 포트폴리오 전체 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<PortfolioDto.Statistics>> getMyPortfolioStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 포트폴리오 통계 조회: 사용자={}", userDetails.getUsername());
        
        PortfolioDto.Statistics statistics = portfolioService.getUserPortfolioStatistics(
                userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 공개 포트폴리오 목록 조회
     */
    @GetMapping("/public")
    @Operation(summary = "공개 포트폴리오 목록", description = "공개된 포트폴리오 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDto.Summary>>> getPublicPortfolios(
            @Parameter(description = "수익률 기준 필터 (예: 'high', 'positive')") 
            @RequestParam(required = false) String performanceFilter,
            @Parameter(description = "정렬 기준 (RETURN_DESC, RETURN_ASC, CREATED_DESC)") 
            @RequestParam(defaultValue = "RETURN_DESC") String sortBy,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("공개 포트폴리오 목록 조회: 필터={}, 정렬={}", performanceFilter, sortBy);
        
        PageResponse<PortfolioDto.Summary> portfolios = portfolioService.getPublicPortfolios(
                performanceFilter, sortBy, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("공개 포트폴리오 목록을 성공적으로 조회했습니다.", portfolios));
    }

    /**
     * 포트폴리오 가격 업데이트
     */
    @PostMapping("/{portfolioId}/refresh-prices")
    @Operation(summary = "포트폴리오 가격 갱신", description = "포트폴리오의 모든 코인 가격을 최신으로 업데이트합니다.")
    public ResponseEntity<ApiResponse<PortfolioDto.Response>> refreshPortfolioPrices(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 가격 갱신: ID={}, 사용자={}", portfolioId, userDetails.getUsername());
        
        PortfolioDto.Response portfolio = portfolioService.refreshPortfolioPrices(
                portfolioId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 가격이 성공적으로 갱신되었습니다.", portfolio));
    }

    /**
     * 포트폴리오 복제
     */
    @PostMapping("/{portfolioId}/clone")
    @Operation(summary = "포트폴리오 복제", description = "기존 포트폴리오를 복제하여 새로운 포트폴리오를 생성합니다.")
    public ResponseEntity<ApiResponse<PortfolioDto.Response>> clonePortfolio(
            @Parameter(description = "복제할 포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Parameter(description = "새 포트폴리오 이름") @RequestParam String newName,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오 복제: 원본ID={}, 새이름={}, 사용자={}", 
                portfolioId, newName, userDetails.getUsername());
        
        PortfolioDto.Response clonedPortfolio = portfolioService.clonePortfolio(
                portfolioId, newName, userDetails.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("포트폴리오가 성공적으로 복제되었습니다.", clonedPortfolio));
    }
}
