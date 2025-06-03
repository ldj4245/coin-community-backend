package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.News;
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
public class NewsDto {

    /**
     * 뉴스 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsResponse {
        private Long id;
        private String title;
        private String content;
        private String source;
        private String author;
        private String url;
        private String imageUrl;
        private LocalDateTime publishedAt;
        private Integer viewCount;
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
    public static class NewsSummaryResponse {
        private Long id;
        private String title;
        private String source;
        private String imageUrl;
        private LocalDateTime publishedAt;
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
