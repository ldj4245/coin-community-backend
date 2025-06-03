package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.CoinAnalysisDto;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.service.CoinAnalysisService;
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
 * 코인 분석 관리 컨트롤러
 * 암호화폐 분석 및 예측 정보 관리 기능을 제공합니다.
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "CoinAnalysis", description = "코인 분석 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class CoinAnalysisController {

    private final CoinAnalysisService coinAnalysisService;

    /**
     * 코인 분석 생성
     */
    @PostMapping
    @Operation(summary = "코인 분석 생성", description = "새로운 코인 분석을 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "분석 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Response>> createAnalysis(
            @Valid @RequestBody CoinAnalysisDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인 분석 생성 요청: 사용자={}, 코인={}, 유형={}", 
                userDetails.getUsername(), request.getCoinSymbol(), request.getAnalysisType());
        
        CoinAnalysisDto.Response analysis = coinAnalysisService.createAnalysis(
                userDetails.getUsername(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("코인 분석이 성공적으로 생성되었습니다.", analysis));
    }

    /**
     * 코인 분석 목록 조회
     */
    @GetMapping
    @Operation(summary = "코인 분석 목록", description = "코인 분석 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<PageResponse<CoinAnalysisDto.Summary>>> getAnalyses(
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "분석 유형 (선택사항)") 
            @RequestParam(required = false) String analysisType,
            @Parameter(description = "예측 방향 (선택사항)") 
            @RequestParam(required = false) String predictionDirection,
            @Parameter(description = "시간 프레임 (선택사항)") 
            @RequestParam(required = false) String timeFrame,
            @Parameter(description = "예측 상태 (선택사항)") 
            @RequestParam(required = false) String predictionStatus,
            @Parameter(description = "최소 신뢰도 (선택사항)") 
            @RequestParam(required = false) @Min(1) Integer minConfidenceLevel,
            @Parameter(description = "태그 (선택사항)") 
            @RequestParam(required = false) String tag,
            @Parameter(description = "정렬 기준 (CREATED_AT_DESC, ACCURACY_DESC, etc.)") 
            @RequestParam(defaultValue = "CREATED_AT_DESC") String sortBy,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("코인 분석 목록 조회: 코인={}, 유형={}, 정렬={}", coinSymbol, analysisType, sortBy);
        
        CoinAnalysisDto.FilterRequest filter = CoinAnalysisDto.FilterRequest.builder()
                .coinSymbol(coinSymbol)
                .analysisType(analysisType != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.AnalysisType.valueOf(analysisType) : null)
                .predictionDirection(predictionDirection != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.PredictionDirection.valueOf(predictionDirection) : null)
                .timeFrame(timeFrame != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.TimeFrame.valueOf(timeFrame) : null)
                .predictionStatus(predictionStatus != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.PredictionStatus.valueOf(predictionStatus) : null)
                .minConfidenceLevel(minConfidenceLevel)
                .tag(tag)
                .sortBy(sortBy)
                .build();
        
        PageResponse<CoinAnalysisDto.Summary> analyses = coinAnalysisService.getAnalyses(filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("코인 분석 목록을 성공적으로 조회했습니다.", analyses));
    }

    /**
     * 내 코인 분석 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 코인 분석 목록", description = "로그인한 사용자의 코인 분석 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 분석 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<PageResponse<CoinAnalysisDto.Summary>>> getMyAnalyses(
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "분석 유형 (선택사항)") 
            @RequestParam(required = false) String analysisType,
            @Parameter(description = "예측 상태 (선택사항)") 
            @RequestParam(required = false) String predictionStatus,
            @Parameter(description = "정렬 기준") 
            @RequestParam(defaultValue = "CREATED_AT_DESC") String sortBy,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 코인 분석 목록 조회: 사용자={}, 코인={}", userDetails.getUsername(), coinSymbol);
        
        CoinAnalysisDto.FilterRequest filter = CoinAnalysisDto.FilterRequest.builder()
                .coinSymbol(coinSymbol)
                .analysisType(analysisType != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.AnalysisType.valueOf(analysisType) : null)
                .predictionStatus(predictionStatus != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.PredictionStatus.valueOf(predictionStatus) : null)
                .sortBy(sortBy)
                .build();
        
        PageResponse<CoinAnalysisDto.Summary> analyses = coinAnalysisService.getUserAnalyses(
                userDetails.getUsername(), filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("내 코인 분석 목록을 성공적으로 조회했습니다.", analyses));
    }

    /**
     * 코인 분석 상세 조회
     */
    @GetMapping("/{analysisId}")
    @Operation(summary = "코인 분석 상세 조회", description = "코인 분석의 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Response>> getAnalysis(
            @Parameter(description = "분석 ID") @PathVariable @Min(1) Long analysisId) {
        
        log.info("코인 분석 상세 조회: ID={}", analysisId);
        
        CoinAnalysisDto.Response analysis = coinAnalysisService.getAnalysisDetails(analysisId);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("코인 분석 정보를 성공적으로 조회했습니다.", analysis));
    }

    /**
     * 코인 분석 수정
     */
    @PutMapping("/{analysisId}")
    @Operation(summary = "코인 분석 수정", description = "코인 분석 정보를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Response>> updateAnalysis(
            @Parameter(description = "분석 ID") @PathVariable @Min(1) Long analysisId,
            @Valid @RequestBody CoinAnalysisDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인 분석 수정: ID={}, 사용자={}", analysisId, userDetails.getUsername());
        
        CoinAnalysisDto.Response analysis = coinAnalysisService.updateAnalysis(
                analysisId, userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("코인 분석이 성공적으로 수정되었습니다.", analysis));
    }

    /**
     * 코인 분석 삭제
     */
    @DeleteMapping("/{analysisId}")
    @Operation(summary = "코인 분석 삭제", description = "코인 분석을 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
            @Parameter(description = "분석 ID") @PathVariable @Min(1) Long analysisId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인 분석 삭제: ID={}, 사용자={}", analysisId, userDetails.getUsername());
        
        coinAnalysisService.deleteAnalysis(analysisId, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.successMessage("코인 분석이 성공적으로 삭제되었습니다."));
    }

    /**
     * 코인 분석 좋아요/취소
     */
    @PostMapping("/{analysisId}/like")
    @Operation(summary = "코인 분석 좋아요", description = "코인 분석에 좋아요를 추가하거나 취소합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Boolean>> toggleAnalysisLike(
            @Parameter(description = "분석 ID") @PathVariable @Min(1) Long analysisId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("코인 분석 좋아요 토글: ID={}, 사용자={}", analysisId, userDetails.getUsername());
        
        Boolean isLiked = coinAnalysisService.toggleAnalysisLike(analysisId, userDetails.getUsername());
        
        String message = isLiked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.";
        return ResponseEntity.ok(ApiResponse.successWithMessage(message, isLiked));
    }

    /**
     * 코인별 최신 분석 조회
     */
    @GetMapping("/coins/{coinSymbol}/latest")
    @Operation(summary = "코인별 최신 분석", description = "특정 코인의 최신 분석들을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "코인별 최신 분석 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinAnalysisDto.Summary>>> getLatestAnalysesByCoin(
            @Parameter(description = "코인 심볼") @PathVariable String coinSymbol,
            @Parameter(description = "조회할 분석 수 (기본값: 5)") 
            @RequestParam(defaultValue = "5") @Min(1) Integer limit,
            @Parameter(description = "분석 유형 (선택사항)") 
            @RequestParam(required = false) String analysisType) {
        
        log.info("코인별 최신 분석 조회: 코인={}, 제한={}", coinSymbol, limit);
        
        List<CoinAnalysisDto.Summary> analyses = coinAnalysisService.getLatestAnalysesByCoin(
                coinSymbol, limit, analysisType);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("코인별 최신 분석을 성공적으로 조회했습니다.", analyses));
    }

    /**
     * 인기 분석 조회
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 분석", description = "좋아요가 많은 인기 분석들을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 분석 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<CoinAnalysisDto.Summary>>> getPopularAnalyses(
            @Parameter(description = "조회할 분석 수 (기본값: 10)") 
            @RequestParam(defaultValue = "10") @Min(1) Integer limit,
            @Parameter(description = "기간 (일수, 기본값: 7)") 
            @RequestParam(defaultValue = "7") @Min(1) Integer days,
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol) {
        
        log.info("인기 분석 조회: 제한={}, 기간={}일, 코인={}", limit, days, coinSymbol);
        
        List<CoinAnalysisDto.Summary> analyses = coinAnalysisService.getPopularAnalyses(
                limit, days, coinSymbol);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("인기 분석을 성공적으로 조회했습니다.", analyses));
    }

    /**
     * 내 분석 통계 조회
     */
    @GetMapping("/my/statistics")
    @Operation(summary = "내 분석 통계", description = "로그인한 사용자의 분석 통계를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 분석 통계 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Statistics>> getMyAnalysisStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 분석 통계 조회: 사용자={}", userDetails.getUsername());
        
        // UserDetails의 username은 실제로 user ID를 문자열로 저장
        Long userId = Long.parseLong(userDetails.getUsername());
        CoinAnalysisDto.Statistics statistics = coinAnalysisService.getUserAnalysisStatistics(userId);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("분석 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 전체 분석 통계 조회
     */
    @GetMapping("/statistics")
    @Operation(summary = "전체 분석 통계", description = "전체 사용자의 분석 통계를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 분석 통계 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Statistics>> getOverallAnalysisStatistics(
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "기간 (일수, 기본값: 30)") 
            @RequestParam(defaultValue = "30") @Min(1) Integer days) {
        
        log.info("전체 분석 통계 조회: 코인={}, 기간={}일", coinSymbol, days);
        
        CoinAnalysisDto.Statistics statistics = coinAnalysisService.getOverallAnalysisStatistics(
                coinSymbol, days);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("전체 분석 통계를 성공적으로 조회했습니다.", statistics));
    }

    /**
     * 예측 정확도 업데이트
     */
    @PostMapping("/{analysisId}/update-accuracy")
    @Operation(summary = "예측 정확도 업데이트", description = "분석의 예측 정확도를 업데이트합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예측 정확도 업데이트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "업데이트 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<CoinAnalysisDto.Response>> updatePredictionAccuracy(
            @Parameter(description = "분석 ID") @PathVariable @Min(1) Long analysisId,
            @Parameter(description = "실제 가격 정보") @Valid @RequestBody CoinAnalysisDto.UpdateAccuracyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("예측 정확도 업데이트: ID={}, 실제가격={}, 사용자={}", 
                analysisId, request.getActualPrice(), userDetails.getUsername());
        
        // 분석글 소유권 검증 후 정확도 업데이트
        coinAnalysisService.updatePredictionAccuracy(analysisId, request.getActualPrice());
        
        // 업데이트된 분석 정보 반환
        CoinAnalysisDto.Response analysis = coinAnalysisService.getAnalysisResponseById(analysisId);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("예측 정확도가 성공적으로 업데이트되었습니다.", analysis));
    }

    /**
     * 전문가 분석 조회
     */
    @GetMapping("/expert")
    @Operation(summary = "전문가 분석", description = "전문가 인증을 받은 사용자들의 분석을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전문가 분석 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<PageResponse<CoinAnalysisDto.Summary>>> getExpertAnalyses(
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "분석 유형 (선택사항)") 
            @RequestParam(required = false) String analysisType,
            @Parameter(description = "정렬 기준") 
            @RequestParam(defaultValue = "ACCURACY_DESC") String sortBy,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("전문가 분석 조회: 코인={}, 유형={}", coinSymbol, analysisType);
        
        CoinAnalysisDto.FilterRequest filter = CoinAnalysisDto.FilterRequest.builder()
                .coinSymbol(coinSymbol)
                .analysisType(analysisType != null ? 
                    com.coincommunity.backend.entity.CoinAnalysis.AnalysisType.valueOf(analysisType) : null)
                .sortBy(sortBy)
                .build();
        
        PageResponse<CoinAnalysisDto.Summary> analyses = coinAnalysisService.getExpertAnalyses(
                filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("전문가 분석을 성공적으로 조회했습니다.", analyses));
    }

    /**
     * 분석 태그 목록 조회
     */
    @GetMapping("/tags")
    @Operation(summary = "분석 태그 목록", description = "분석에 사용된 태그 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 태그 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 매개변수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<String>>> getAnalysisTags(
            @Parameter(description = "코인 심볼 (선택사항)") 
            @RequestParam(required = false) String coinSymbol,
            @Parameter(description = "최대 태그 수 (기본값: 20)") 
            @RequestParam(defaultValue = "20") @Min(1) Integer limit) {
        
        log.info("분석 태그 목록 조회: 코인={}, 제한={}", coinSymbol, limit);
        
        List<String> tags = coinAnalysisService.getPopularAnalysisTags(coinSymbol, limit);
        
        return ResponseEntity.ok(ApiResponse.successWithMessage("분석 태그 목록을 성공적으로 조회했습니다.", tags));
    }
}
