package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.News;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 정보 관련 DTO 클래스
 */
@Schema(description = "뉴스 기사 정보")
public class NewsDto {

    /**
     * 뉴스 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "뉴스 응답 DTO")
    public static class NewsResponse {
        @Schema(description = "기사 ID", example = "1")
        private Long id;
        @Schema(description = "기사 제목", example = "비트코인, 새로운 최고가 경신")
        private String title;
        private String content;
        @Schema(description = "언론사", example = "코인데스크코리아")
        private String source;
        private String author;
        @Schema(description = "기사 원문 링크", example = "https://example.com/news/1")
        private String url;
        private String imageUrl;
        @Schema(description = "발행일")
        private LocalDateTime publishedAt;
        @Schema(description = "조회수", example = "100")
        private Integer viewCount;
        @Schema(description = "등록일")
        private LocalDateTime createdAt;
        
        /**
         * News 엔티티로부터 NewsResponse DTO를 생성합니다.
         */
        public static NewsResponse from(News news) {
            return NewsResponse.builder()
                    .id(news.getId())
                    .title(news.getTitle())
                    .content(news.getContent())
                    .source(news.getSource())
                    .author(news.getAuthor())
                    .url(news.getUrl())
                    .imageUrl(news.getImageUrl())
                    .publishedAt(news.getPublishedAt())
                    .viewCount(news.getViewCount())
                    .createdAt(news.getCreatedAt())
                    .build();
        }
        
        /**
         * News 엔티티 목록으로부터 NewsResponse DTO 목록을 생성합니다.
         */
        public static List<NewsResponse> fromList(List<News> newsList) {
            return newsList.stream()
                    .map(NewsResponse::from)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 뉴스 요약 응답 DTO (목록 조회용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "뉴스 요약 응답 DTO (목록 조회용)")
    public static class NewsSummaryResponse {
        @Schema(description = "기사 ID", example = "1")
        private Long id;
        @Schema(description = "기사 제목", example = "비트코인, 새로운 최고가 경신")
        private String title;
        @Schema(description = "언론사", example = "코인데스크코리아")
        private String source;
        private String imageUrl;
        @Schema(description = "발행일")
        private LocalDateTime publishedAt;
        @Schema(description = "조회수", example = "100")
        private Integer viewCount;
        
        /**
         * News 엔티티로부터 NewsSummaryResponse DTO를 생성합니다.
         */
        public static NewsSummaryResponse from(News news) {
            return NewsSummaryResponse.builder()
                    .id(news.getId())
                    .title(news.getTitle())
                    .source(news.getSource())
                    .imageUrl(news.getImageUrl())
                    .publishedAt(news.getPublishedAt())
                    .viewCount(news.getViewCount())
                    .build();
        }
        
        /**
         * News 엔티티 목록으로부터 NewsSummaryResponse DTO 목록을 생성합니다.
         */
        public static List<NewsSummaryResponse> fromList(List<News> newsList) {
            return newsList.stream()
                    .map(NewsSummaryResponse::from)
                    .collect(Collectors.toList());
        }
    }
}
