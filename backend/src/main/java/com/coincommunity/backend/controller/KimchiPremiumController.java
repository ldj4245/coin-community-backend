package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.ExchangeRateTableDto;
import com.coincommunity.backend.dto.KimchiPremiumDto;
import com.coincommunity.backend.service.KimchiPremiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 김치프리미엄 및 거래소 시세 컨트롤러
 */
@RestController
@RequestMapping("/kimchi-premium")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "김치프리미엄", description = "김치프리미엄 및 거래소별 시세 관련 API")
public class KimchiPremiumController {

    private final KimchiPremiumService kimchiPremiumService;

    @GetMapping("/{symbol}")
    @Operation(
        summary = "특정 코인의 김치프리미엄 조회",
        description = "국내외 거래소 가격을 비교하여 김치프리미엄을 계산합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<KimchiPremiumDto>> getKimchiPremium(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("김치프리미엄 조회 요청 - 심볼: {}", symbol);

        try {
            KimchiPremiumDto kimchiPremium = kimchiPremiumService.getKimchiPremium(symbol.toUpperCase());
            
            if (kimchiPremium != null) {
                return ResponseEntity.ok(ApiResponse.successWithMessage(
                    "김치프리미엄 조회 성공",
                    kimchiPremium
                ));
            } else {
                return ResponseEntity.notFound()
                    .build();
            }

        } catch (Exception e) {
            log.error("김치프리미엄 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("김치프리미엄 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(
        summary = "전체 코인 김치프리미엄 목록 조회",
        description = "주요 코인들의 김치프리미엄을 한번에 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<KimchiPremiumDto>>> getAllKimchiPremiums() {
        log.info("전체 김치프리미엄 목록 조회 요청");

        try {
            List<KimchiPremiumDto> kimchiPremiums = kimchiPremiumService.getAllKimchiPremiums();
            
            return ResponseEntity.ok(ApiResponse.successWithMessage(
                "전체 김치프리미엄 조회 성공",
                kimchiPremiums
            ));

        } catch (Exception e) {
            log.error("전체 김치프리미엄 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("김치프리미엄 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/exchange-rates/{symbol}")
    @Operation(
        summary = "거래소별 시세표 조회",
        description = "특정 코인의 국내외 거래소별 상세 시세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코인을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<ExchangeRateTableDto>> getExchangeRateTable(
            @Parameter(description = "코인 심볼", example = "BTC")
            @PathVariable String symbol
    ) {
        log.info("거래소별 시세표 조회 요청 - 심볼: {}", symbol);

        try {
            ExchangeRateTableDto exchangeRateTable = kimchiPremiumService.getExchangeRateTable(symbol.toUpperCase());
            
            if (exchangeRateTable != null) {
                return ResponseEntity.ok(ApiResponse.successWithMessage(
                    "거래소별 시세표 조회 성공",
                    exchangeRateTable
                ));
            } else {
                return ResponseEntity.notFound()
                    .build();
            }

        } catch (Exception e) {
            log.error("거래소별 시세표 조회 중 오류 발생 - 심볼: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("시세표 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
    @Operation(
        summary = "김치프리미엄 API 테스트",
        description = "김치프리미엄 계산 기능을 테스트합니다. 업비트 등 거래소 연동 상태를 확인합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<String>> testKimchiPremium() {
        log.info("김치프리미엄 API 테스트");

        try {
            // BTC로 테스트 - 업비트 형식으로 요청
            KimchiPremiumDto testResult = kimchiPremiumService.getKimchiPremium("KRW-BTC");

            if (testResult != null) {
                // 각 거래소 연결 상태 확인
                StringBuilder statusMsg = new StringBuilder();
                statusMsg.append("BTC 김치프리미엄: ").append(testResult.getPremiumRate())
                         .append("% (").append(testResult.getPremiumAmount()).append("원)\n\n");

                statusMsg.append("거래소 상태:\n");

                // 업비트 상태 확인
                boolean hasUpbit = testResult.getDomesticPrices().containsKey("UPBIT");
                statusMsg.append("- 업비트: ").append(hasUpbit ? "연결됨" : "연결 실패");
                if (hasUpbit) {
                    statusMsg.append(" (").append(testResult.getDomesticPrices().get("UPBIT").getPriceKrw()).append("원)");
                }
                statusMsg.append("\n");

                // 다른 거래소들도 확인
                testResult.getDomesticPrices().forEach((exchange, info) -> {
                    if (!exchange.equals("UPBIT")) {
                        statusMsg.append("- ").append(exchange).append(": 연결됨")
                                 .append(" (").append(info.getPriceKrw()).append("원)\n");
                    }
                });

                testResult.getForeignPrices().forEach((exchange, info) -> {
                    statusMsg.append("- ").append(exchange).append(": 연결됨")
                             .append(" (").append(info.getPriceKrw()).append("원)\n");
                });

                return ResponseEntity.ok(ApiResponse.successWithMessage(
                    "김치프리미엄 API 테스트 성공" + (hasUpbit ? "" : " (업비트 미연결)"),
                    statusMsg.toString()
                ));
            } else {
                return ResponseEntity.ok(ApiResponse.successWithMessage(
                    "김치프리미엄 API 연동 성공하였으나 데이터가 없습니다",
                    "현재 김치프리미엄 데이터를 조회할 수 없습니다."
                ));
            }

        } catch (Exception e) {
            log.error("김치프리미엄 API 테스트 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("API 테스트 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/supported-coins")
    @Operation(
        summary = "지원하는 코인 목록 조회",
        description = "김치프리미엄 계산이 가능한 코인 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<String[]>> getSupportedCoins() {
        log.info("지원하는 코인 목록 조회");
        
        String[] supportedCoins = {"BTC", "ETH", "XRP", "ADA", "DOT", "LINK", "LTC", "BCH"};
        
        return ResponseEntity.ok(ApiResponse.successWithMessage(
            "지원 코인 목록 조회 성공",
            supportedCoins
        ));
    }

    @GetMapping("/health")
    @Operation(
        summary = "김치프리미엄 API 건강 상태 확인",
        description = "김치프리미엄 계산 서비스 및 거래소 연결 상태를 확인합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        log.info("김치프리미엄 API 건강 상태 확인 요청");

        try {
            Map<String, Object> healthData = new HashMap<>();

            // 전체 서비스 상태
            healthData.put("status", "UP");
            healthData.put("timestamp", LocalDateTime.now());

            // 각 거래소 연결 상태 확인
            Map<String, Object> exchangeStatus = new HashMap<>();

            // BTC로 테스트
            KimchiPremiumDto testResult = kimchiPremiumService.getKimchiPremium("BTC");

            if (testResult != null) {
                // 국내 거래소 상태
                testResult.getDomesticPrices().forEach((exchange, info) -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("connected", true);
                    status.put("lastUpdated", info.getLastUpdated());
                    status.put("price", info.getPriceKrw());
                    exchangeStatus.put(exchange, status);
                });

                // 해외 거래소 상태
                testResult.getForeignPrices().forEach((exchange, info) -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("connected", true);
                    status.put("lastUpdated", info.getLastUpdated());
                    status.put("price", info.getPriceKrw());
                    exchangeStatus.put(exchange, status);
                });

                // 업비트가 없으면 추가
                if (!testResult.getDomesticPrices().containsKey("UPBIT")) {
                    Map<String, Object> upbitStatus = new HashMap<>();
                    upbitStatus.put("connected", false);
                    upbitStatus.put("lastUpdated", LocalDateTime.now());
                    upbitStatus.put("error", "업비트 API 연결 실패");
                    exchangeStatus.put("UPBIT", upbitStatus);
                }
            } else {
                // 서비스 자체가 응답하지 않는 경우
                Map<String, Object> errorStatus = new HashMap<>();
                errorStatus.put("connected", false);
                errorStatus.put("lastUpdated", LocalDateTime.now());
                errorStatus.put("error", "김치프리미엄 서비스 응답 없음");

                exchangeStatus.put("UPBIT", errorStatus);
                exchangeStatus.put("BITHUMB", errorStatus);
                exchangeStatus.put("COINONE", errorStatus);
                exchangeStatus.put("KORBIT", errorStatus);
                exchangeStatus.put("BINANCE", errorStatus);
            }

            healthData.put("exchanges", exchangeStatus);

            return ResponseEntity.ok(ApiResponse.successWithMessage(
                "김치프리미엄 API 건강 상태 확인 성공",
                healthData
            ));

        } catch (Exception e) {
            log.error("김치프리미엄 API 건강 상태 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("건강 상태 확인 실패: " + e.getMessage()));
        }
    }
}
