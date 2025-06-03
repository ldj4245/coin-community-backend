// package com.coincommunity.backend.controller;

// import com.coincommunity.backend.dto.ApiResponse;
// import com.coincommunity.backend.dto.NewsDto;
// import com.coincommunity.backend.dto.PageResponse;
// import com.coincommunity.backend.service.NewsService;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.Parameter;
// import io.swagger.v3.oas.annotations.responses.ApiResponses;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.web.PageableDefault;
// import org.springframework.format.annotation.DateTimeFormat;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.List;

// /**
//  * 뉴스 관련 API 엔드포인트
//  * 기본 경로: /api/news
//  */
// @RestController
// @RequestMapping("/news")
// @RequiredArgsConstructor
// @Tag(name = "뉴스", description = "암호화폐 관련 뉴스 제공 API")
// public class NewsController {

//     private final NewsService newsService;

//     /**
//      * 모든 뉴스 목록 조회 (페이징)
//      */
//     @Operation(
//         summary = "뉴스 목록 조회",
//         description = "암호화폐 관련 모든 뉴스의 목록을 페이징 처리하여 조회합니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping
//     public ResponseEntity<ApiResponse<PageResponse<NewsDto.NewsSummaryResponse>>> getAllNews(
//             @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 기준)") 
//             @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable) {
        
//         Page<NewsDto.NewsSummaryResponse> page = newsService.getAllNews(pageable);
//         return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
//     }

//     /**
//      * 뉴스 상세 정보 조회
//      */
//     @Operation(
//         summary = "뉴스 상세 조회",
//         description = "특정 뉴스의 상세 정보를 조회합니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/{id}")
//     public ResponseEntity<ApiResponse<NewsDto.NewsResponse>> getNewsById(
//             @Parameter(description = "뉴스 ID") @PathVariable Long id) {
        
//         NewsDto.NewsResponse response = newsService.getNewsDetail(id);
//         return ResponseEntity.ok(ApiResponse.success(response));
//     }

//     /**
//      * 뉴스 검색
//      */
//     @Operation(
//         summary = "뉴스 검색",
//         description = "키워드로 뉴스를 검색합니다. 제목과 내용에서 키워드를 포함한 뉴스를 찾습니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/search")
//     public ResponseEntity<ApiResponse<PageResponse<NewsDto.NewsSummaryResponse>>> searchNews(
//             @Parameter(description = "검색할 키워드") @RequestParam String keyword,
//             @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 기준)") 
//             @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable) {
        
//         Page<NewsDto.NewsSummaryResponse> page = newsService.searchNews(keyword, pageable);
//         return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
//     }

//     /**
//      * 특정 출처의 뉴스 조회
//      */
//     @Operation(
//         summary = "특정 출처의 뉴스 조회",
//         description = "특정 뉴스 출처(웨사이트)의 뉴스만 필터링하여 조회합니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 출처의 뉴스를 찾을 수 없음"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/by-source/{source}")
//     public ResponseEntity<ApiResponse<PageResponse<NewsDto.NewsSummaryResponse>>> getNewsBySource(
//             @Parameter(description = "뉴스 출처명") @PathVariable String source,
//             @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 기준)") 
//             @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable) {
        
//         Page<NewsDto.NewsSummaryResponse> page = newsService.getNewsBySource(source, pageable);
//         return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
//     }

//     /**
//      * 특정 기간 내의 뉴스 조회
//      */
//     @Operation(
//         summary = "특정 기간 내 뉴스 조회",
//         description = "지정한 시작일과 종료일 사이에 게시된 뉴스만 조회합니다. 날짜 형식은 ISO 8601 형식(YYYY-MM-DDThh:mm:ss)을 따릅니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/by-date")
//     public ResponseEntity<ApiResponse<PageResponse<NewsDto.NewsSummaryResponse>>> getNewsByDateRange(
//             @Parameter(description = "시작 날짜와 시간(ISO 8601: YYYY-MM-DDThh:mm:ss)") 
//             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
//             @Parameter(description = "종료 날짜와 시간(ISO 8601: YYYY-MM-DDThh:mm:ss)") 
//             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
//             @Parameter(description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 기준)") 
//             @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable) {
        
//         Page<NewsDto.NewsSummaryResponse> page = newsService.getNewsByDateRange(start, end, pageable);
//         return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
//     }

//     /**
//      * 최근 뉴스 목록 조회
//      */
//     @Operation(
//         summary = "최근 뉴스 목록",
//         description = "최근에 게시된 뉴스를 조회합니다. 일반적으로 가장 최근 게시된 10개의 뉴스를 제공합니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/recent")
//     public ResponseEntity<ApiResponse<List<NewsDto.NewsSummaryResponse>>> getRecentNews() {
//         List<NewsDto.NewsSummaryResponse> news = newsService.getRecentNews();
//         return ResponseEntity.ok(ApiResponse.success(news));
//     }

//     /**
//      * 인기 뉴스 목록 조회
//      */
//     @Operation(
//         summary = "인기 뉴스 목록",
//         description = "조회수, 좋아요 등을 기준으로 인기 뉴스를 조회합니다.",
//         tags = {"뉴스"}
//     )
//     @ApiResponses({
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
//         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
//     })
//     @GetMapping("/popular")
//     public ResponseEntity<ApiResponse<List<NewsDto.NewsSummaryResponse>>> getPopularNews() {
//         List<NewsDto.NewsSummaryResponse> news = newsService.getPopularNews();
//         return ResponseEntity.ok(ApiResponse.success(news));
//     }


// }
