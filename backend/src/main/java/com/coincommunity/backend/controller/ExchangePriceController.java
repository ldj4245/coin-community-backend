package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.ExchangeComparisonDto;
import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.service.ExchangePriceService;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategyFactory;
import com.coincommunity.backend.external.exchange.ExchangeApiStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 거래소별 시세 컨트롤러
 */
@RestController
@RequestMapping("/exchange-prices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "거래소 시세", description = "거래소별 암호화폐 시세 조회 API")
public class ExchangePriceController {

    private final ExchangePriceService exchangePriceService;
    private final ExchangeApiStrategyFactory strategyFactory;

    @GetMapping("/debug/strategies")
    @Operation(
        summary = "등록된 거래소 전략 목록 조회 (디버깅용)",
        description = "현재 등록된 모든 거래소 API 전략들의 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegisteredStrategies() {
        log.info("등록된 거래소 전략 목록 조회 요청");

        try {
            List<ExchangeApiStrategy> allStrategies = strategyFactory.getAllStrategies();
            List<ExchangeApiStrategy> domesticStrategies = strategyFactory.getDomesticStrategies();
            List<ExchangeApiStrategy> foreignStrategies = strategyFactory.getForeignStrategies();
            List<ExchangeApiStrategy> healthyStrategies = strategyFactory.getHealthyStrategies();

            List<Map<String, Object>> strategyInfo = allStrategies.stream()
                .map(strategy -> {
                    return Map.of(
                        "exchangeName", strategy.getExchangeName(),
                        "exchangeType", strategy.getExchangeType().toString(),
                        "isHealthy", strategy.isHealthy(),
                        "supportedCoins", strategy.getSupportedCoins()
                    );
                })
                .collect(Collectors.toList());

            Map<String, Object> result = Map.of(
                "totalStrategies", allStrategies.size(),
                "domesticStrategies", domesticStrategies.size(),
                "foreignStrategies", foreignStrategies.size(),
                "healthyStrategies", healthyStrategies.size(),
                "strategies", strategyInfo
            );

            log.info("등록된 전략 수: 전체={}, 국내={}, 해외={}, 정상={}",
                allStrategies.size(), domesticStrategies.size(), foreignStrategies.size(), healthyStrategies.size());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("등록된 전략 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("전략 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{symbol}")
    @Operation(
        summary = "특정 코인의 거래소별 시세 조회",
        description = "국내외 모든 거래소의 특정 코인 시세를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<ExchangePriceDto>>> getExchangePrices(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("거래소별 시세 조회 요청 - 심볼: {}", symbol);

        try {
            List<ExchangePriceDto> prices = exchangePriceService.getExchangePrices(symbol.toUpperCase());
            
            if (!prices.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(prices));
            } else {
                return ResponseEntity.ok(ApiResponse.success(prices));
            }

        } catch (Exception e) {
            log.error("거래소별 시세 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("시세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/comparison/{symbol}")
    @Operation(
        summary = "특정 코인의 거래소별 시세 비교",
        description = "특정 코인의 거래소별 시세를 비교하고 김치프리미엄, 최고가/최저가 등의 정보를 제공합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<ExchangeComparisonDto>> getExchangeComparison(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("거래소별 시세 비교 조회 요청 - 심볼: {}", symbol);

        try {
            ExchangeComparisonDto comparison = exchangePriceService.getExchangeComparison(symbol.toUpperCase());
            
            if (comparison != null) {
                return ResponseEntity.ok(ApiResponse.success(comparison));
            } else {
                return ResponseEntity.notFound()
                    .build();
            }

        } catch (Exception e) {
            log.error("거래소별 시세 비교 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("시세 비교 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/comparison")
    @Operation(
        summary = "모든 코인의 거래소별 시세 비교",
        description = "지원하는 모든 코인의 거래소별 시세 비교 정보를 김치프리미엄 순으로 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<ExchangeComparisonDto>>> getAllExchangeComparisons() {
        log.info("모든 코인 거래소별 시세 비교 조회 요청");

        try {
            // 주요 코인들에 대한 시세 비교 정보 조회
            List<String> majorCoins = List.of("BTC", "ETH", "XRP", "ADA", "DOT");
            List<ExchangeComparisonDto> comparisons = majorCoins.stream()
                .map(exchangePriceService::getExchangeComparison)
                .filter(java.util.Objects::nonNull)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(comparisons));

        } catch (Exception e) {
            log.error("모든 코인 거래소별 시세 비교 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("시세 비교 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/domestic/{symbol}")
    @Operation(
        summary = "특정 코인의 국내 거래소 시세만 조회",
        description = "업비트, 빗썸, 코인원, 코빗 등 국내 거래소의 시세만 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<ExchangePriceDto>>> getDomesticExchangePrices(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("국내 거래소 시세 조회 요청 - 심볼: {}", symbol);

        try {
            List<ExchangePriceDto> allPrices = exchangePriceService.getExchangePrices(symbol.toUpperCase());
            List<ExchangePriceDto> domesticPrices = allPrices.stream()
                .filter(price -> price.getExchangeType() == ExchangePriceDto.ExchangeType.DOMESTIC)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(domesticPrices));

        } catch (Exception e) {
            log.error("국내 거래소 시세 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("국내 거래소 시세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/foreign/{symbol}")
    @Operation(
        summary = "특정 코인의 해외 거래소 시세만 조회",
        description = "바이낸스 등 해외 거래소의 시세만 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<ExchangePriceDto>>> getForeignExchangePrices(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("해외 거래소 시세 조회 요청 - 심볼: {}", symbol);

        try {
            List<ExchangePriceDto> allPrices = exchangePriceService.getExchangePrices(symbol.toUpperCase());
            List<ExchangePriceDto> foreignPrices = allPrices.stream()
                .filter(price -> price.getExchangeType() == ExchangePriceDto.ExchangeType.FOREIGN)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success(foreignPrices));

        } catch (Exception e) {
            log.error("해외 거래소 시세 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("해외 거래소 시세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/supported-coins")
    @Operation(
        summary = "지원하는 코인 목록 조회",
        description = "거래소별 시세 조회가 가능한 코인 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<String>>> getSupportedCoins() {
        log.info("지원하는 코인 목록 조회");
        
        List<String> supportedCoins = exchangePriceService.getAllSupportedCoins();
        
        return ResponseEntity.ok(ApiResponse.success(supportedCoins));
    }

    @GetMapping("/test")
    @Operation(
        summary = "거래소 시세 API 테스트",
        description = "거래소별 시세 조회 기능을 테스트합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "테스트 실패")
    })
    public ResponseEntity<ApiResponse<String>> testExchangePrices() {
        log.info("거래소 시세 API 테스트");
        
        try {
            // BTC로 테스트
            List<ExchangePriceDto> testResult = exchangePriceService.getExchangePrices("BTC");
            
            if (!testResult.isEmpty()) {
                ExchangePriceDto highestPrice = testResult.stream()
                    .max((a, b) -> a.getCurrentPrice().compareTo(b.getCurrentPrice()))
                    .orElse(null);
                
                String message = "거래소 시세 API 테스트 성공 - 총 " + testResult.size() + "개 거래소 조회됨. 최고가: " + 
                    (highestPrice != null ? highestPrice.getExchangeKoreanName() + " " + highestPrice.getCurrentPrice() + "원" : "N/A");
                
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                return ResponseEntity.ok(ApiResponse.success("거래소 시세 API 연동 성공하였으나 데이터가 없습니다"));
            }

        } catch (Exception e) {
            log.error("거래소 시세 API 테스트 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("API 테스트 실패: " + e.getMessage()));
        }
    }
}
