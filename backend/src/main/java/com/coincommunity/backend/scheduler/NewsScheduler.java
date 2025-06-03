package com.coincommunity.backend.scheduler;

import com.coincommunity.backend.entity.News;
import com.coincommunity.backend.external.news.GuardianApiClient;
import com.coincommunity.backend.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 정보를 주기적으로 가져오는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final GuardianApiClient guardianApiClient;
    private final NewsService newsService;
    
    /**
     * 1시간마다 암호화폐 관련 뉴스를 가져옵니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void fetchCryptoNews() {
        try {
            log.info("암호화폐 뉴스 데이터 가져오기 시작");
            List<News> allNewsList = new ArrayList<>();
            
            // Guardian API를 통해 뉴스 수집
            try {
                List<News> guardianNews = guardianApiClient.fetchCryptoNews();
                if (!guardianNews.isEmpty()) {
                    allNewsList.addAll(guardianNews);
                    log.info("Guardian에서 {}개 뉴스를 가져왔습니다.", guardianNews.size());
                } else {
                    log.warn("Guardian API에서 뉴스를 가져오지 못했습니다.");
                }
            } catch (Exception e) {
                log.error("Guardian API를 통한 뉴스 수집 중 오류가 발생했습니다", e);
            }
            
            // 뉴스 저장 (중복 체크 포함)
            if (!allNewsList.isEmpty()) {
                List<News> savedNews = newsService.saveAllNews(allNewsList);
                log.info("암호화폐 뉴스 데이터 저장 완료: {}개 뉴스", savedNews.size());
            } else {
                log.warn("가져온 뉴스 데이터가 없습니다.");
            }
        } catch (Exception e) {
            log.error("뉴스 데이터 가져오기 중 오류가 발생했습니다", e);
        }
    }
}
