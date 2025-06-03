package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 한국 코인 뉴스 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "한국 코인 뉴스 정보")
public class KoreanNewsDto {

    @Schema(description = "뉴스 ID")
    private String newsId;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "내용 요약")
    private String description;

    @Schema(description = "원문 URL")
    private String url;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "발행처")
    private String source;

    @Schema(description = "작성자")
    private String author;

    @Schema(description = "발행일시")
    private LocalDateTime publishedAt;

    @Schema(description = "관련 코인 심볼들")
    private String[] relatedCoins;

    @Schema(description = "뉴스 카테고리", example = "MARKET")
    private NewsCategory category;

    @Schema(description = "중요도 점수 (1-10)", example = "7")
    private Integer importanceScore;

    @Schema(description = "감정 분석 결과", example = "POSITIVE")
    private SentimentType sentiment;

    public enum NewsCategory {
        MARKET("시장"),
        REGULATION("규제"),
        TECHNOLOGY("기술"),
        COMPANY("기업"),
        GLOBAL("해외"),
        DOMESTIC("국내"),
        ANALYSIS("분석");

        private final String koreanName;

        NewsCategory(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    public enum SentimentType {
        POSITIVE("긍정"),
        NEGATIVE("부정"),
        NEUTRAL("중립");

        private final String koreanName;

        SentimentType(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }
}
