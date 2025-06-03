package com.coincommunity.backend.external.news;

import com.coincommunity.backend.entity.News;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Guardian API를 통해 암호화폐 관련 뉴스를 조회하는 클라이언트
 * https://open-platform.theguardian.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianApiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.guardian.api-key:test}")
    private String apiKey;
    
    @Value("${external.guardian.base-url:https://content.guardianapis.com}")
    private String baseUrl;
    
    /**
     * 최신 암호화폐 관련 뉴스를 조회합니다.
     */
    public List<News> fetchCryptoNews() {
        try {
            // 검색 쿼리를 더 유연하게 변경
            String url = baseUrl + "/search" +
                    "?q=(cryptocurrency OR bitcoin OR ethereum OR blockchain OR crypto OR \"digital currency\")" +
                    "&show-fields=thumbnail,bodyText" +
                    "&page-size=20" +
                    "&order-by=newest" +
                    "&api-key=" + apiKey;
            
            log.info("Guardian API 호출: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            
            log.info("Guardian API 응답 길이: {}", response != null ? response.length() : 0);
            log.info("Guardian API 실제 응답: {}", response);
            
            if (response != null) {
                List<News> result = parseGuardianResponse(response);
                log.info("파싱된 뉴스 개수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.error("Guardian API 호출 중 예외가 발생했습니다", e);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Guardian API 응답을 News 엔티티 목록으로 변환합니다.
     */
    private List<News> parseGuardianResponse(String response) {
        List<News> newsList = new ArrayList<>();
        
        try {
            log.info("Guardian API 응답 파싱 시작");
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode responseNode = rootNode.get("response");
            
            if (responseNode != null && "ok".equals(responseNode.get("status").asText())) {
                JsonNode resultsNode = responseNode.get("results");
                log.info("results 노드 존재: {}, 배열인가: {}", resultsNode != null, resultsNode != null && resultsNode.isArray());
                
                if (resultsNode != null && resultsNode.isArray()) {
                    log.info("결과 배열 크기: {}", resultsNode.size());
                    for (JsonNode article : resultsNode) {
                        try {
                            News news = new News();
                            
                            String title = article.get("webTitle").asText();
                            String url = article.get("webUrl").asText();
                            
                            log.info("뉴스 처리 중: {}", title);
                            
                            news.setTitle(title);
                            news.setUrl(url);
                            news.setSource("The Guardian");
                            
                            // 내용 설정 (fields가 있을 경우)
                            JsonNode fieldsNode = article.get("fields");
                            if (fieldsNode != null) {
                                JsonNode bodyText = fieldsNode.get("bodyText");
                                if (bodyText != null) {
                                    String content = bodyText.asText();
                                    // 내용이 너무 길면 자르기
                                    if (content.length() > 500) {
                                        content = content.substring(0, 500) + "...";
                                    }
                                    news.setContent(content);
                                } else {
                                    news.setContent(news.getTitle()); // 내용이 없으면 제목으로 대체
                                }
                                
                                // 이미지 URL 설정
                                JsonNode thumbnail = fieldsNode.get("thumbnail");
                                if (thumbnail != null) {
                                    news.setImageUrl(thumbnail.asText());
                                }
                            } else {
                                news.setContent(news.getTitle());
                            }
                            
                            // 섹션명을 작성자로 사용
                            news.setAuthor(article.get("sectionName").asText());
                            
                            // 발행일 파싱
                            String publishedDate = article.get("webPublicationDate").asText();
                            try {
                                ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishedDate);
                                news.setPublishedAt(zonedDateTime.toLocalDateTime());
                            } catch (Exception e) {
                                log.warn("날짜 파싱 실패: {}", publishedDate);
                                news.setPublishedAt(LocalDateTime.now());
                            }
                            
                            news.setViewCount(0);
                            
                            newsList.add(news);
                            log.info("뉴스 추가됨: {}", news.getTitle());
                        } catch (Exception e) {
                            log.warn("Guardian 뉴스 항목을 변환하는 중 오류가 발생했습니다", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Guardian API 응답을 파싱하는 중 오류가 발생했습니다", e);
        }
        
        return newsList;
    }
}
