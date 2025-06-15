package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 뉴스 관련 API 엔드포인트
 * 기본 경로: /news
 */
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "뉴스", description = "암호화폐 관련 뉴스 제공 API")
public class NewsController {

    /**
     * 뉴스 목록 조회 (임시 구현)
     */
    @Operation(
        summary = "뉴스 목록 조회",
        description = "암호화폐 관련 뉴스의 목록을 조회합니다.",
        tags = {"뉴스"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllNews() {
        log.info("뉴스 목록 조회 요청");
        
        try {
            // 임시 뉴스 데이터
            List<Map<String, Object>> mockNews = List.of(
                Map.of(
                    "id", 1L,
                    "title", "비트코인, 새로운 최고가 경신",
                    "summary", "비트코인이 새로운 역사적 최고가를 기록하며 암호화폐 시장에 활기를 불어넣고 있습니다.",
                    "source", "CoinDesk",
                    "publishedAt", LocalDateTime.now().minusHours(2).toString(),
                    "url", "https://example.com/news/1"
                ),
                Map.of(
                    "id", 2L,
                    "title", "이더리움 2.0 업그레이드 완료",
                    "summary", "이더리움의 메이저 업그레이드가 성공적으로 완료되어 네트워크 효율성이 크게 향상되었습니다.",
                    "source", "CoinTelegraph",
                    "publishedAt", LocalDateTime.now().minusHours(4).toString(),
                    "url", "https://example.com/news/2"
                ),
                Map.of(
                    "id", 3L,
                    "title", "중앙은행 디지털화폐(CBDC) 도입 논의 가속화",
                    "summary", "전 세계 중앙은행들이 디지털화폐 도입을 위한 연구와 시범 운영을 확대하고 있습니다.",
                    "source", "Reuters",
                    "publishedAt", LocalDateTime.now().minusHours(6).toString(),
                    "url", "https://example.com/news/3"
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(mockNews));
            
        } catch (Exception e) {
            log.error("뉴스 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("뉴스 목록을 불러오는 중 오류가 발생했습니다."));
        }
    }

    /**
     * 인기 뉴스 조회
     */
    @Operation(
        summary = "인기 뉴스 조회",
        description = "인기도가 높은 암호화폐 뉴스를 조회합니다.",
        tags = {"뉴스"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPopularNews() {
        log.info("인기 뉴스 조회 요청");
        
        try {
            // 임시 인기 뉴스 데이터
            List<Map<String, Object>> popularNews = List.of(
                Map.of(
                    "id", 4L,
                    "title", "암호화폐 규제 프레임워크 발표",
                    "summary", "정부가 암호화폐 시장의 건전한 발전을 위한 새로운 규제 방안을 발표했습니다.",
                    "source", "한국경제",
                    "publishedAt", LocalDateTime.now().minusHours(1).toString(),
                    "url", "https://example.com/news/4",
                    "viewCount", 15420,
                    "likes", 342
                ),
                Map.of(
                    "id", 5L,
                    "title", "DeFi 프로토콜 보안 강화 방안",
                    "summary", "탈중앙화 금융(DeFi) 플랫폼들이 보안성 향상을 위한 새로운 기술을 도입하고 있습니다.",
                    "source", "CoinDesk",
                    "publishedAt", LocalDateTime.now().minusHours(3).toString(),
                    "url", "https://example.com/news/5",
                    "viewCount", 12890,
                    "likes", 278
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(popularNews));
            
        } catch (Exception e) {
            log.error("인기 뉴스 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("인기 뉴스를 불러오는 중 오류가 발생했습니다."));
        }
    }

    /**
     * 뉴스 검색
     */
    @Operation(
        summary = "뉴스 검색",
        description = "키워드로 뉴스를 검색합니다.",
        tags = {"뉴스"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchNews(
            @RequestParam String keyword) {
        log.info("뉴스 검색 요청 - 키워드: {}", keyword);
        
        try {
            // 임시 검색 결과
            List<Map<String, Object>> searchResults = List.of(
                Map.of(
                    "id", 6L,
                    "title", keyword + " 관련 최신 동향",
                    "summary", keyword + "에 대한 최신 정보와 시장 분석을 제공합니다.",
                    "source", "CryptoNews",
                    "publishedAt", LocalDateTime.now().minusHours(2).toString(),
                    "url", "https://example.com/news/6",
                    "relevance", 0.95
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(searchResults));
            
        } catch (Exception e) {
            log.error("뉴스 검색 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("뉴스 검색 중 오류가 발생했습니다."));
        }
    }
}
