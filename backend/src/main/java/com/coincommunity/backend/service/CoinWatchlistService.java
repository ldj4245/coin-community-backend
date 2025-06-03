package com.coincommunity.backend.service;

import com.coincommunity.backend.entity.CoinWatchlist;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.repository.CoinWatchlistRepository;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.dto.CoinWatchlistDto;
import com.coincommunity.backend.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 코인 관심종목 서비스
 * 30년차 베테랑 개발자의 아키텍처 패턴 적용:
 * - 개인화된 투자 관심종목 관리
 * - 실시간 가격 알림 시스템
 * - 사용자 맞춤형 포트폴리오 추천
 * - 시장 동향 분석 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinWatchlistService {

    private final CoinWatchlistRepository coinWatchlistRepository;
    private final UserRepository userRepository;
    private final CoinPriceService coinPriceService;
    // private final UserScoreService userScoreService;
    // private final NotificationService notificationService;

    /**
     * 관심종목 추가
     */
    @Transactional
    @CacheEvict(value = {"userWatchlist", "watchlistStats"}, key = "#userId")
    public CoinWatchlist addToWatchlist(Long userId, String coinId, String coinName, String coinSymbol,
                                       CoinWatchlist.WatchlistCategory category, BigDecimal targetHighPrice,
                                       BigDecimal targetLowPrice, String memo, boolean isAlertEnabled) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 중복 등록 체크
        if (coinWatchlistRepository.existsByUserIdAndCoinId(userId, coinId)) {
            throw new IllegalArgumentException("이미 관심종목에 등록된 코인입니다.");
        }

        // 현재 가격 조회
        BigDecimal currentPrice = coinPriceService.getCurrentPrice(coinId);

        CoinWatchlist watchlist = CoinWatchlist.builder()
                .user(user)
                .coinId(coinId)
                .coinName(coinName)
                .coinSymbol(coinSymbol)
                .category(category)
                .currentPrice(currentPrice)
                .targetHighPrice(targetHighPrice)
                .targetLowPrice(targetLowPrice)
                .memo(memo)
                .alertEnabled(isAlertEnabled)
                .build();

        CoinWatchlist savedWatchlist = coinWatchlistRepository.save(watchlist);

        // 사용자 활동 점수 추가
        // userScoreService.addActivityScore(userId, 5);

        log.info("관심종목 추가 완료: userId={}, coinId={}, category={}", userId, coinId, category);
        return savedWatchlist;
    }

    /**
     * 관심종목 수정
     */
    @Transactional
    @CacheEvict(value = {"userWatchlist", "watchlistDetail"}, key = "#userId")
    public CoinWatchlist updateWatchlist(Long watchlistId, Long userId, CoinWatchlist.WatchlistCategory category,
                                        BigDecimal targetHighPrice, BigDecimal targetLowPrice, 
                                        String memo, boolean isAlertEnabled) {
        
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, userId);

        watchlist.setCategory(category);
        watchlist.setTargetHighPrice(targetHighPrice);
        watchlist.setTargetLowPrice(targetLowPrice);
        watchlist.setMemo(memo);
        watchlist.setAlertEnabled(isAlertEnabled);

        CoinWatchlist updatedWatchlist = coinWatchlistRepository.save(watchlist);
        log.info("관심종목 수정 완료: watchlistId={}, userId={}", watchlistId, userId);

        return updatedWatchlist;
    }

    /**
     * 관심종목 제거
     */
    @Transactional
    @CacheEvict(value = {"userWatchlist", "watchlistStats"}, key = "#userId")
    public void removeFromWatchlist(Long watchlistId, Long userId) {
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, userId);

        coinWatchlistRepository.delete(watchlist);
        log.info("관심종목 제거 완료: watchlistId={}, userId={}, coinId={}", 
                watchlistId, userId, watchlist.getCoinId());
    }

    /**
     * 관심종목 일괄 제거
     */
    @Transactional
    @CacheEvict(value = {"userWatchlist", "watchlistStats"}, key = "#userId")
    public void removeMultipleFromWatchlist(List<Long> watchlistIds, Long userId) {
        for (Long watchlistId : watchlistIds) {
            removeFromWatchlist(watchlistId, userId);
        }
        log.info("관심종목 일괄 제거 완료: userId={}, count={}", userId, watchlistIds.size());
    }

    /**
     * 사용자 관심종목 목록 조회
     */
    @Cacheable(value = "userWatchlist", key = "#userId")
    public List<CoinWatchlist> getUserWatchlist(Long userId) {
        List<CoinWatchlist> watchlist = coinWatchlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // 현재 가격 업데이트
        updateCurrentPrices(watchlist);
        
        return watchlist;
    }

    /**
     * 카테고리별 관심종목 조회
     */
    public List<CoinWatchlist> getWatchlistByCategory(Long userId, CoinWatchlist.WatchlistCategory category) {
        List<CoinWatchlist> watchlist = coinWatchlistRepository.findByUserIdAndCategory(userId, category);
        updateCurrentPrices(watchlist);
        return watchlist;
    }

    /**
     * 알림 활성화된 관심종목 조회
     */
    public List<CoinWatchlist> getAlertEnabledWatchlist(Long userId) {
        List<CoinWatchlist> watchlist = coinWatchlistRepository.findByUserIdAndIsAlertEnabledTrue(userId);
        updateCurrentPrices(watchlist);
        return watchlist;
    }

    /**
     * 특정 관심종목 상세 조회
     */
    @Cacheable(value = "watchlistDetail", key = "#watchlistId")
    public CoinWatchlist getWatchlistDetail(Long watchlistId, Long userId) {
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, userId);
        
        // 현재 가격 업데이트
        BigDecimal currentPrice = coinPriceService.getCurrentPrice(watchlist.getCoinId());
        watchlist.setCurrentPrice(currentPrice);
        
        return watchlist;
    }

    // DTO 기반 메서드들 추가

    /**
     * 관심종목 추가 (DTO 기반)
     */
    @Transactional
    public CoinWatchlistDto.Response addToWatchlist(String username, CoinWatchlistDto.AddRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        CoinWatchlist watchlist = addToWatchlist(user.getId(), request.getCoinSymbol(), 
                request.getCoinSymbol(), request.getCoinSymbol(), 
                CoinWatchlist.WatchlistCategory.valueOf(request.getCategory().toUpperCase()),
                request.getTargetHighPrice(), request.getTargetLowPrice(), 
                request.getMemo(), request.getIsAlertEnabled() != null ? request.getIsAlertEnabled() : false);
        
        return convertToResponseDto(watchlist);
    }

    /**
     * 사용자 관심종목 목록 조회 (DTO 기반)
     */
    public PageResponse<CoinWatchlistDto.Response> getUserWatchlist(String username, 
                                                                   CoinWatchlistDto.FilterRequest filter, 
                                                                   org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        List<CoinWatchlist> watchlists = getUserWatchlist(user.getId());
        List<CoinWatchlistDto.Response> responses = watchlists.stream()
                .map(this::convertToResponseDto)
                .collect(java.util.stream.Collectors.toList());
        
        return PageResponse.of(responses, pageable, (long) responses.size());
    }

    /**
     * 관심종목 상세 조회 (DTO 기반)
     */
    public CoinWatchlistDto.Response getWatchlistItem(Long watchlistId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, user.getId());
        
        return convertToResponseDto(watchlist);
    }

    /**
     * 관심종목 수정 (DTO 기반)
     */
    @Transactional
    public CoinWatchlistDto.Response updateWatchlistItem(Long watchlistId, String username, 
                                                        CoinWatchlistDto.UpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, user.getId());
        
        if (request.getTargetHighPrice() != null) {
            watchlist.setTargetHighPrice(request.getTargetHighPrice());
        }
        if (request.getTargetLowPrice() != null) {
            watchlist.setTargetLowPrice(request.getTargetLowPrice());
        }
        if (request.getMemo() != null) {
            watchlist.setMemo(request.getMemo());
        }
        if (request.getIsAlertEnabled() != null) {
            watchlist.setAlertEnabled(request.getIsAlertEnabled());
        }
        
        CoinWatchlist updatedWatchlist = coinWatchlistRepository.save(watchlist);
        return convertToResponseDto(updatedWatchlist);
    }

    /**
     * 관심종목 삭제 (DTO 기반)
     */
    @Transactional
    public void removeFromWatchlist(Long watchlistId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, user.getId());
        
        coinWatchlistRepository.delete(watchlist);
        log.info("관심종목 삭제 완료: watchlistId={}, username={}", watchlistId, username);
    }

    /**
     * 코인별 관심종목 삭제 (DTO 기반)
     */
    @Transactional
    public void removeFromWatchlistByCoin(String username, String coinSymbol) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        removeFromWatchlist(user.getId(), coinSymbol);
    }

    /**
     * 가격 알림 설정 (DTO 기반)
     */
    @Transactional
    public CoinWatchlistDto.Response setPriceAlert(Long watchlistId, String username, 
                                                  CoinWatchlistDto.PriceAlertRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        CoinWatchlist watchlist = getWatchlistById(watchlistId);
        validateWatchlistOwnership(watchlist, user.getId());
        
        watchlist.setTargetHighPrice(request.getTargetHighPrice());
        watchlist.setTargetLowPrice(request.getTargetLowPrice());
        watchlist.setAlertEnabled(true);
        
        CoinWatchlist updatedWatchlist = coinWatchlistRepository.save(watchlist);
        return convertToResponseDto(updatedWatchlist);
    }

    /**
     * 사용자 관심종목 통계 (DTO 기반)
     */
    public CoinWatchlistDto.Statistics getUserWatchlistStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        List<CoinWatchlist> watchlists = getUserWatchlist(user.getId());
        
        long totalItems = watchlists.size();
        long alertItems = watchlists.stream()
                .mapToLong(w -> w.isAlertEnabled() ? 1 : 0)
                .sum();
        
        return CoinWatchlistDto.Statistics.builder()
                .totalItems(totalItems)
                .priceAlertEnabledCount(alertItems)
                .totalWatchlistItems(totalItems)
                .gainersCount(watchlists.stream()
                        .mapToLong(w -> w.getPriceChangePercent() != null && 
                                      w.getPriceChangePercent().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                        .sum())
                .losersCount(watchlists.stream()
                        .mapToLong(w -> w.getPriceChangePercent() != null && 
                                      w.getPriceChangePercent().compareTo(BigDecimal.ZERO) < 0 ? 1 : 0)
                        .sum())
                .build();
    }

    /**
     * 사용자 관심종목 카테고리 목록 (DTO 기반)
     */
    public List<String> getUserWatchlistCategories(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        return getUserWatchlist(user.getId()).stream()
                .map(w -> w.getCategory().name())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 트리거된 가격 알림 조회 (DTO 기반)
     */
    public List<CoinWatchlistDto.Response> getTriggeredPriceAlerts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        // 임시 구현: 빈 리스트 반환
        return java.util.Collections.emptyList();
    }

    /**
     * 관심종목 가격 새로고침 (DTO 기반)
     */
    @Transactional
    public List<CoinWatchlistDto.Summary> refreshWatchlistPrices(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        List<CoinWatchlist> watchlists = getUserWatchlist(user.getId());
        
        // 가격 업데이트 후 Summary DTO 변환
        return watchlists.stream()
                .map(this::convertToSummaryDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 인기 관심종목 코인 조회 (DTO 기반)
     */
    public List<CoinWatchlistDto.Summary> getPopularWatchlistCoins(Integer limit, Integer days) {
        // 임시 구현: 빈 리스트 반환
        return java.util.Collections.emptyList();
    }

    /**
     * 대량 관심종목 추가 (DTO 기반)
     */
    @Transactional
    public List<CoinWatchlistDto.Response> addBulkToWatchlist(String username, List<String> coinSymbols, String category) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        return coinSymbols.stream()
                .map(symbol -> {
                    CoinWatchlist watchlist = addToWatchlist(user.getId(), symbol, symbol, symbol,
                            CoinWatchlist.WatchlistCategory.valueOf(category.toUpperCase()),
                            null, null, null, false);
                    return convertToResponseDto(watchlist);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 대량 관심종목 삭제 (DTO 기반)
     */
    @Transactional
    public void removeBulkFromWatchlist(String username, List<Long> watchlistIds) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        watchlistIds.forEach(id -> removeFromWatchlist(id, username));
    }

    // DTO 변환 메서드들

    private CoinWatchlistDto.Response convertToResponseDto(CoinWatchlist watchlist) {
        return CoinWatchlistDto.Response.builder()
                .id(watchlist.getId())
                .coinSymbol(watchlist.getCoinSymbol())
                .coinName(watchlist.getCoinName())
                .category(watchlist.getCategory().name())
                .currentPrice(watchlist.getCurrentPrice())
                .targetHighPrice(watchlist.getTargetHighPrice())
                .targetLowPrice(watchlist.getTargetLowPrice())
                .priceChangePercent(watchlist.getPriceChangePercent())
                .notes(watchlist.getMemo())
                .priceAlertEnabled(watchlist.isAlertEnabled())
                .lastTriggeredAt(watchlist.getLastTriggeredAt())
                .createdAt(watchlist.getCreatedAt())
                .updatedAt(watchlist.getUpdatedAt())
                .build();
    }

    private CoinWatchlistDto.Summary convertToSummaryDto(CoinWatchlist watchlist) {
        return CoinWatchlistDto.Summary.builder()
                .id(watchlist.getId())
                .coinSymbol(watchlist.getCoinSymbol())
                .currentPrice(watchlist.getCurrentPrice())
                .priceChangePercent(watchlist.getPriceChangePercent())
                .category(watchlist.getCategory().name())
                .build();
    }

    // Helper 메서드

    private CoinWatchlist getWatchlistById(Long watchlistId) {
        return coinWatchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("관심종목을 찾을 수 없습니다: " + watchlistId));
    }

    private void validateWatchlistOwnership(CoinWatchlist watchlist, Long userId) {
        if (!watchlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("관심종목에 대한 권한이 없습니다.");
        }
    }

    /**
     * 현재 가격 정보 업데이트
     */
    private void updateCurrentPrices(List<CoinWatchlist> watchlist) {
        log.debug("관심종목 가격 정보 업데이트 시작: {} 개 종목", watchlist.size());
        
        for (CoinWatchlist item : watchlist) {
            try {
                // CoinPriceService를 통해 현재 가격 조회
                BigDecimal currentPrice = coinPriceService.getCurrentPrice(item.getCoinSymbol());
                
                if (currentPrice != null) {
                    BigDecimal previousPrice = item.getCurrentPrice();
                    item.setCurrentPrice(currentPrice);
                    
                    // 가격 변동률 계산
                    if (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal changePercent = currentPrice.subtract(previousPrice)
                                .divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        item.setPriceChangePercent(changePercent);
                    }
                    
                    log.debug("가격 업데이트 완료: {} = {}", item.getCoinSymbol(), currentPrice);
                } else {
                    log.warn("가격 정보를 가져올 수 없습니다: {}", item.getCoinSymbol());
                }
            } catch (Exception e) {
                log.error("가격 업데이트 실패: {} - {}", item.getCoinSymbol(), e.getMessage());
            }
        }
    }
}
