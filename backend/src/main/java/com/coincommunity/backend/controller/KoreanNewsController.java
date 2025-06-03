//package com.coincommunity.backend.controller;
//
//import com.coincommunity.backend.dto.ApiResponse;
//import com.coincommunity.backend.dto.KoreanNewsDto;
//import com.coincommunity.backend.dto.PageResponse;
//import com.coincommunity.backend.service.KoreanNewsService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 한국 코인 뉴스 컨트롤러
// * TODO: 한국 뉴스 API 결정 후 구현 예정
// */
//@RestController
//@RequestMapping("/korean-news")
//@RequiredArgsConstructor
//@Slf4j
//@Tag(name = "한국 뉴스", description = "한국 암호화폐 뉴스 관련 API (TODO: API 미정)")
//public class KoreanNewsController {
//
//    private final KoreanNewsService koreanNewsService;
//
//    @GetMapping
//    @Operation(
//        summary = "한국 코인 뉴스 목록 조회 (미구현)",
//        description = "TODO: 한국 뉴스 API 결정 후 구현 예정"
//    )
//    @ApiResponses(value = {
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//    })
//    public ResponseEntity<ApiResponse<PageResponse<KoreanNewsDto>>> getKoreanNews(
//            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
//            @Parameter(description = "카테고리 필터") @RequestParam(required = false) String category) {
//
//        log.info("한국 뉴스 조회 요청 - 페이지: {}, 크기: {}, 카테고리: {}", page, size, category);
//
//        PageResponse<KoreanNewsDto> response = koreanNewsService.getKoreanCoinNews(page, size, category);
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }
//
//    @GetMapping("/search")
//    @Operation(
//        summary = "한국 뉴스 검색 (미구현)",
//        description = "TODO: 키워드로 한국 뉴스 검색"
//    )
//    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 검색 키워드"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "외부 뉴스 API 연결 실패")
//    })
//    public ResponseEntity<ApiResponse<PageResponse<KoreanNewsDto>>> searchKoreanNews(
//            @Parameter(description = "검색 키워드") @RequestParam String keyword,
//            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
//
//        log.info("한국 뉴스 검색 요청 - 키워드: {}, 페이지: {}, 크기: {}", keyword, page, size);
//
//        PageResponse<KoreanNewsDto> response = koreanNewsService.searchKoreanNews(keyword, page, size);
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }
//
//    @GetMapping("/category/{category}")
//    @Operation(
//        summary = "카테고리별 한국 뉴스 조회 (미구현)",
//        description = "TODO: 특정 카테고리의 한국 뉴스 조회"
//    )
//    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리별 뉴스 조회 성공"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 카테고리 매개변수"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 카테고리의 뉴스를 찾을 수 없음"),
//        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
//    })
//    public ResponseEntity<ApiResponse<PageResponse<KoreanNewsDto>>> getNewsByCategory(
//            @Parameter(description = "뉴스 카테고리") @PathVariable String category,
//            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
//            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
//
//        log.info("카테고리별 한국 뉴스 조회 요청 - 카테고리: {}, 페이지: {}, 크기: {}", category, page, size);
//
//        PageResponse<KoreanNewsDto> response = koreanNewsService.getNewsByCategory(category, page, size);
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }
//}
