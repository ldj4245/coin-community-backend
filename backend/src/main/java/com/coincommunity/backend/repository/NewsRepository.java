package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    /**
     * 뉴스 제목으로 검색합니다.
     */
    Page<News> findByTitleContaining(String keyword, Pageable pageable);
    
    /**
     * 특정 출처의 뉴스를 조회합니다.
     */
    Page<News> findBySource(String source, Pageable pageable);
    
    /**
     * 특정 기간 내에 발행된 뉴스를 조회합니다.
     */
    Page<News> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 최근 뉴스 목록을 조회합니다.
     */
    List<News> findTop10ByOrderByPublishedAtDesc();
    
    /**
     * 인기 뉴스 목록을 조회합니다.
     */
    List<News> findTop10ByOrderByViewCountDesc();
    
    /**
     * URL로 뉴스 존재 여부를 확인합니다.
     */
    boolean existsByUrl(String url);
    
    /**
     * 제목과 출처로 뉴스 존재 여부를 확인합니다.
     */
    boolean existsByTitleAndSource(String title, String source);
}
