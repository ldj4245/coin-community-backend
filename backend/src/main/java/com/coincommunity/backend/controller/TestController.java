package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.entity.News;
import com.coincommunity.backend.external.news.GuardianApiClient;
import com.coincommunity.backend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 테스트 및 개발용 API 엔드포인트
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "테스트", description = "개발 및 테스트용 API")
@Slf4j
public class TestController {

    private final GuardianApiClient guardianApiClient;
    private final NewsService newsService;

    /**
     * Guardian API 테스트
     */
    @Operation(
        summary = "Guardian API 테스트",
        description = "Guardian API를 통해 뉴스를 가져와서 테스트합니다.",
        tags = {"테스트"}
    )
    @PostMapping("/guardian-news")
    public ResponseEntity<ApiResponse<String>> testGuardianApi() {
        try {
            log.info("Guardian API 테스트 시작");
            List<News> newsList = guardianApiClient.fetchCryptoNews();
            
            if (!newsList.isEmpty()) {
                List<News> savedNews = newsService.saveAllNews(newsList);
                String message = String.format("Guardian API를 통해 %d개의 뉴스를 성공적으로 가져와서 저장했습니다.", savedNews.size());
                log.info(message);
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                String message = "Guardian API에서 뉴스를 가져왔지만 저장할 데이터가 없습니다.";
                log.warn(message);
                return ResponseEntity.ok(ApiResponse.success(message));
            }
        } catch (Exception e) {
            String message = "Guardian API 테스트 중 오류가 발생했습니다: " + e.getMessage();
            log.error(message, e);
            return ResponseEntity.ok(ApiResponse.error(message, null));
        }
    }

    /**
     * 뉴스 데이터 수동 동기화
     */
    @Operation(
        summary = "뉴스 데이터 수동 동기화",
        description = "스케줄러를 기다리지 않고 즉시 뉴스 데이터를 동기화합니다.",
        tags = {"테스트"}
    )
    @PostMapping("/sync-news")
    public ResponseEntity<ApiResponse<String>> syncNews() {
        try {
            log.info("수동 뉴스 동기화 시작");
            List<News> newsList = guardianApiClient.fetchCryptoNews();
            
            if (!newsList.isEmpty()) {
                List<News> savedNews = newsService.saveAllNews(newsList);
                String message = String.format("뉴스 동기화 완료: %d개의 뉴스가 저장되었습니다.", savedNews.size());
                log.info(message);
                return ResponseEntity.ok(ApiResponse.success(message));
            } else {
                String message = "동기화할 뉴스 데이터가 없습니다.";
                log.warn(message);
                return ResponseEntity.ok(ApiResponse.success(message));
            }
        } catch (Exception e) {
            String message = "뉴스 동기화 중 오류가 발생했습니다: " + e.getMessage();
            log.error(message, e);
            return ResponseEntity.ok(ApiResponse.error(message, null));
        }
    }
}
