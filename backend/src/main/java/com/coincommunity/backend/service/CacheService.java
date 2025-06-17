package com.coincommunity.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    public void evictAllKimchiPremiumCaches() {
        log.info("김치프리미엄 관련 캐시를 모두 삭제합니다.");
        evictCache("kimchiPremium");
        evictCache("kimchiPremiumList");
        evictCache("exchangeRateTable");
    }

    private void evictCache(String cacheName) {
        try {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.info("'{}' 캐시가 성공적으로 삭제되었습니다.", cacheName);
        } catch (NullPointerException e) {
            log.warn("'{}' 캐시를 찾을 수 없습니다.", cacheName);
        }
    }
} 