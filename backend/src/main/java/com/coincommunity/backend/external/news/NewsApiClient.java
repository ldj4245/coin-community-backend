package com.coincommunity.backend.external.news;

import com.coincommunity.backend.entity.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * News API를 통해 암호화폐 관련 뉴스를 조회하는 클라이언트
 * https://newsapi.org/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.newsapi.key:}")
    private String apiKey;
    
    @Value("${external.newsapi.base-url:https://newsapi.org/v2}")
    private String baseUrl;
    
    /**
     * 최신 암호화폐 관련 뉴스를 조회합니다.
     */
    public List<News> fetchCryptoNews() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("News API 키가 설정되지 않았습니다.");
            return new ArrayList<>();
        }
        
        try {
            String url = baseUrl + "/everything?q=cryptocurrency+OR+bitcoin+OR+ethereum&language=en&sortBy=publishedAt&apiKey=" + apiKey;
            NewsApiResponse response = restTemplate.getForObject(url, NewsApiResponse.class);
            
            if (response != null && "ok".equalsIgnoreCase(response.getStatus())) {
                return convertToNewsEntities(response.getArticles());
            } else {
                log.error("뉴스 데이터를 가져오는 중 오류가 발생했습니다: {}", 
                    response != null ? response.getStatus() : "응답이 null입니다.");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("뉴스 API 호출 중 예외가 발생했습니다", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * API 응답을 News 엔티티 목록으로 변환합니다.
     */
    private List<News> convertToNewsEntities(List<Map<String, Object>> articles) {
        List<News> newsList = new ArrayList<>();
        
        for (Map<String, Object> article : articles) {
            try {
                News news = new News();
                news.setTitle((String) article.get("title"));
                news.setContent((String) article.get("content"));
                news.setSource((String) ((Map<String, Object>) article.get("source")).get("name"));
                news.setAuthor((String) article.get("author"));
                news.setUrl((String) article.get("url"));
                news.setImageUrl((String) article.get("urlToImage"));
                news.setPublishedAt(LocalDateTime.parse(((String) article.get("publishedAt")).replace("Z", "")));
                news.setViewCount(0);
                
                newsList.add(news);
            } catch (Exception e) {
                log.warn("뉴스 항목을 변환하는 중 오류가 발생했습니다", e);
            }
        }
        
        return newsList;
    }
}
