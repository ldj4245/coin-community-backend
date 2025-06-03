package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.TransactionDto;
import com.coincommunity.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 내역 관리 컨트롤러
 * 암호화폐 매수/매도 거래 내역 관리 기능을 제공합니다.
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Transaction", description = "거래 내역 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 거래 내역 생성
     */
    @PostMapping
    @Operation(summary = "거래 내역 생성", description = "새로운 거래 내역을 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "거래 내역 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<TransactionDto.Response>> createTransaction(
            @Valid @RequestBody TransactionDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 생성 요청: 사용자={}, 코인={}, 유형={}", 
                userDetails.getUsername(), request.getCoinSymbol(), request.getTransactionType());
        
        TransactionDto.Response transaction = transactionService.createTransaction(
                userDetails.getUsername(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("거래 내역이 성공적으로 생성되었습니다.", transaction));
    }

    /**
     * 내 거래 내역 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 거래 내역 목록", description = "로그인한 사용자의 거래 내역 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto.Response>>> getMyTransactions(
            @Parameter(description = "포트폴리오 ID (선택사항)") 
            @RequestParam(required = false) @Min(1) Long portfolioId,
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "거래 유형 (선택사항)") 
            @RequestParam(required = false) String transactionType,
            @Parameter(description = "시작일 (선택사항)") 
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료일 (선택사항)") 
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 거래 내역 조회: 사용자={}, 포트폴리오ID={}, 코인={}", 
                userDetails.getUsername(), portfolioId, coinSymbol);
        
        TransactionDto.FilterRequest filter = TransactionDto.FilterRequest.builder()
                .coinSymbol(coinSymbol)
                .transactionType(transactionType != null ? 
                    com.coincommunity.backend.entity.Transaction.TransactionType.valueOf(transactionType) : null)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        PageResponse<TransactionDto.Response> transactions = transactionService.getUserTransactions(
                userDetails.getUsername(), portfolioId, filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("거래 내역을 성공적으로 조회했습니다.", transactions));
    }

    /**
     * 거래 내역 상세 조회
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "거래 내역 상세 조회", description = "거래 내역의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<TransactionDto.Response>> getTransaction(
            @Parameter(description = "거래 ID") @PathVariable @Min(1) Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 상세 조회: ID={}, 사용자={}", transactionId, userDetails.getUsername());
        
        TransactionDto.Response transaction = transactionService.getTransactionDetails(
                transactionId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("거래 내역을 성공적으로 조회했습니다.", transaction));
    }

    /**
     * 거래 내역 수정
     */
    @PutMapping("/{transactionId}")
    @Operation(summary = "거래 내역 수정", description = "거래 내역 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<TransactionDto.Response>> updateTransaction(
            @Parameter(description = "거래 ID") @PathVariable @Min(1) Long transactionId,
            @Valid @RequestBody TransactionDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 수정: ID={}, 사용자={}", transactionId, userDetails.getUsername());
        
        TransactionDto.Response transaction = transactionService.updateTransaction(
                transactionId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("거래 내역이 성공적으로 수정되었습니다.", transaction));
    }

    /**
     * 거래 내역 삭제
     */
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "거래 내역 삭제", description = "거래 내역을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @Parameter(description = "거래 ID") @PathVariable @Min(1) Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 삭제: ID={}, 사용자={}", transactionId, userDetails.getUsername());
        
        transactionService.deleteTransaction(transactionId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successMessage("거래 내역이 성공적으로 삭제되었습니다."));
    }

    /**
     * 포트폴리오별 거래 내역 조회
     */
    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "포트폴리오별 거래 내역", description = "특정 포트폴리오의 거래 내역을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto.Response>>> getPortfolioTransactions(
            @Parameter(description = "포트폴리오 ID") @PathVariable @Min(1) Long portfolioId,
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "거래 유형 (선택사항)") 
            @RequestParam(required = false) String transactionType,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("포트폴리오별 거래 내역 조회: 포트폴리오ID={}, 사용자={}", 
                portfolioId, userDetails.getUsername());
        
        TransactionDto.FilterRequest filter = TransactionDto.FilterRequest.builder()
                .coinSymbol(coinSymbol)
                .transactionType(transactionType != null ? 
                    com.coincommunity.backend.entity.Transaction.TransactionType.valueOf(transactionType) : null)
                .build();
        
        PageResponse<TransactionDto.Response> transactions = transactionService.getPortfolioTransactions(
                portfolioId, userDetails.getUsername(), filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("포트폴리오 거래 내역을 성공적으로 조회했습니다.", transactions));
    }

    /**
     * 거래 내역 통계 조회
     */
    @GetMapping("/my/statistics")
    @Operation(summary = "내 거래 통계", description = "로그인한 사용자의 거래 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<TransactionDto.Summary>> getMyTransactionSummary(
            @Parameter(description = "포트폴리오 ID (선택사항)") 
            @RequestParam(required = false) @Min(1) Long portfolioId,
            @Parameter(description = "통계 기간 (일수, 기본값: 30)") 
            @RequestParam(defaultValue = "30") @Min(1) Integer days,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 거래 통계 조회: 사용자={}, 포트폴리오ID={}, 기간={}일", 
                userDetails.getUsername(), portfolioId, days);
        
        TransactionDto.Summary summary = transactionService.getUserTransactionSummary(
                userDetails.getUsername(), portfolioId, days);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("거래 통계를 성공적으로 조회했습니다.", summary));
    }

    /**
     * 기간별 거래 통계 조회
     */
    @GetMapping("/my/statistics/period")
    @Operation(summary = "기간별 거래 통계", description = "특정 기간의 상세한 거래 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<List<TransactionDto.Statistics>>> getPeriodStatistics(
            @Parameter(description = "시작일") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료일") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "그룹 단위 (DAILY, WEEKLY, MONTHLY)") 
            @RequestParam(defaultValue = "DAILY") String groupBy,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("기간별 거래 통계 조회: 사용자={}, 시작일={}, 종료일={}, 그룹={}", 
                userDetails.getUsername(), startDate, endDate, groupBy);
        
        List<TransactionDto.Statistics> statistics = transactionService.getPeriodStatistics(
                userDetails.getUsername(), startDate, endDate, groupBy);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("기간별 거래 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 코인별 거래 통계 조회
     */
    @GetMapping("/my/statistics/coins")
    @Operation(summary = "코인별 거래 통계", description = "코인별 거래 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<List<TransactionDto.Statistics>>> getCoinStatistics(
            @Parameter(description = "통계 기간 (일수, 기본값: 30)") 
            @RequestParam(defaultValue = "30") @Min(1) Integer days,
            @Parameter(description = "상위 N개 코인 (기본값: 10)") 
            @RequestParam(defaultValue = "10") @Min(1) Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인별 거래 통계 조회: 사용자={}, 기간={}일, 제한={}", 
                userDetails.getUsername(), days, limit);
        
        List<TransactionDto.Statistics> statistics = transactionService.getCoinStatistics(
                userDetails.getUsername(), days, limit);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("코인별 거래 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 거래 내역 일괄 가져오기 (CSV 업로드)
     */
    @PostMapping("/import")
    @Operation(summary = "거래 내역 일괄 가져오기", description = "CSV 파일로 거래 내역을 일괄 등록합니다.")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> importTransactions(
            @Parameter(description = "포트폴리오 ID") @RequestParam @Min(1) Long portfolioId,
            @Parameter(description = "CSV 데이터") @RequestBody String csvData,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 일괄 가져오기: 포트폴리오ID={}, 사용자={}", 
                portfolioId, userDetails.getUsername());
        
        List<TransactionDto.Response> transactions = transactionService.importTransactionsFromCsv(
                portfolioId, userDetails.getUsername(), csvData);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("거래 내역이 성공적으로 가져와졌습니다.", transactions));
    }

    /**
     * 거래 내역 내보내기 (CSV 다운로드)
     */
    @GetMapping("/export")
    @Operation(summary = "거래 내역 내보내기", description = "거래 내역을 CSV 형태로 내보냅니다.")
    public ResponseEntity<ApiResponse<String>> exportTransactions(
            @Parameter(description = "포트폴리오 ID (선택사항)") 
            @RequestParam(required = false) @Min(1) Long portfolioId,
            @Parameter(description = "시작일 (선택사항)") 
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료일 (선택사항)") 
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("거래 내역 내보내기: 사용자={}, 포트폴리오ID={}", 
                userDetails.getUsername(), portfolioId);
        
        String csvData = transactionService.exportTransactionsToCsv(
                userDetails.getUsername(), portfolioId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("거래 내역을 성공적으로 내보냈습니다.", csvData));
    }
}
