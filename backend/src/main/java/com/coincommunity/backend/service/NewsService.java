package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.NewsDto;
import com.coincommunity.backend.entity.News;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NewsService {
    
    private final NewsRepository newsRepository;
    
    /**
     * 뉴스 ID로 뉴스를 조회합니다.
     */
    public News findById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("뉴스를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 뉴스 ID로 뉴스 상세 정보를 조회하고 조회수를 증가시킵니다.
     */
    @Transactional
    public NewsDto.NewsResponse getNewsDetail(Long id) {
        News news = findById(id);
        news.increaseViewCount();
        News updatedNews = newsRepository.save(news);
        return NewsDto.NewsResponse.from(updatedNews);
    }
    
    /**
     * 모든 뉴스를 페이징하여 조회합니다.
     */
    public Page<NewsDto.NewsSummaryResponse> getAllNews(Pageable pageable) {
        Page<News> newsPage = newsRepository.findAll(pageable);
        return newsPage.map(NewsDto.NewsSummaryResponse::from);
    }
    
    /**
     * 키워드로 뉴스를 검색합니다.
     */
    public Page<NewsDto.NewsSummaryResponse> searchNews(String keyword, Pageable pageable) {
        Page<News> newsPage = newsRepository.findByTitleContaining(keyword, pageable);
        return newsPage.map(NewsDto.NewsSummaryResponse::from);
    }
    
    /**
     * 특정 출처의 뉴스를 조회합니다.
     */
    public Page<NewsDto.NewsSummaryResponse> getNewsBySource(String source, Pageable pageable) {
        Page<News> newsPage = newsRepository.findBySource(source, pageable);
        return newsPage.map(NewsDto.NewsSummaryResponse::from);
    }
    
    /**
     * 특정 기간 내의 뉴스를 조회합니다.
     */
    public Page<NewsDto.NewsSummaryResponse> getNewsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<News> newsPage = newsRepository.findByPublishedAtBetween(start, end, pageable);
        return newsPage.map(NewsDto.NewsSummaryResponse::from);
    }
    
    /**
     * 최근 뉴스를 조회합니다.
     */
    public List<NewsDto.NewsSummaryResponse> getRecentNews() {
        List<News> newsList = newsRepository.findTop10ByOrderByPublishedAtDesc();
        return NewsDto.NewsSummaryResponse.fromList(newsList);
    }
    
    /**
     * 인기 뉴스를 조회합니다.
     */
    public List<NewsDto.NewsSummaryResponse> getPopularNews() {
        List<News> newsList = newsRepository.findTop10ByOrderByViewCountDesc();
        return NewsDto.NewsSummaryResponse.fromList(newsList);
    }
    
    /**
     * 외부 API로부터 얻은 뉴스를 저장합니다.
     */
    @Transactional
    public News saveNews(News news) {
        return newsRepository.save(news);
    }
    
    /**
     * 외부 API로부터 얻은 여러 뉴스를 일괄 저장합니다.
     * 중복 검사를 통해 이미 존재하는 뉴스는 저장하지 않습니다.
     */
    @Transactional
    public List<News> saveAllNews(List<News> newsList) {
        List<News> savedNews = new ArrayList<>();
        
        for (News news : newsList) {
            // URL 기반 중복 체크
            if (!newsRepository.existsByUrl(news.getUrl())) {
                // 제목과 출처 기반 추가 중복 체크
                if (!newsRepository.existsByTitleAndSource(news.getTitle(), news.getSource())) {
                    try {
                        News saved = newsRepository.save(news);
                        savedNews.add(saved);
                    } catch (Exception e) {
                        log.warn("뉴스 저장 중 오류가 발생했습니다. 제목: {}, URL: {}", news.getTitle(), news.getUrl(), e);
                    }
                } else {
                    log.debug("중복 뉴스 (제목+출처): {}", news.getTitle());
                }
            } else {
                log.debug("중복 뉴스 (URL): {}", news.getUrl());
            }
        }
        
        log.info("총 {}개 뉴스 중 {}개가 새로 저장되었습니다.", newsList.size(), savedNews.size());
        return savedNews;
    }
}
