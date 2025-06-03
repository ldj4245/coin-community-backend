package com.coincommunity.backend.service;

import com.coincommunity.backend.entity.AnalysisBookmark;
import com.coincommunity.backend.entity.AnalysisLike;
import com.coincommunity.backend.entity.CoinAnalysis;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.repository.AnalysisBookmarkRepository;
import com.coincommunity.backend.repository.AnalysisLikeRepository;
import com.coincommunity.backend.repository.CoinAnalysisRepository;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.dto.CoinAnalysisDto;
import com.coincommunity.backend.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;

/**
 * 코인 분석 서비스
 * 30년차 베테랑 개발자의 아키텍처 패턴 적용:
 * - 전문적인 투자 분석 플랫폼
 * - 사용자 생성 컨텐츠(UGC) 관리
 * - 예측 정확도 추적 시스템
 * - 커뮤니티 기반 평가 시스템
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinAnalysisService {

    private final CoinAnalysisRepository coinAnalysisRepository;
    private final UserRepository userRepository;
    private final AnalysisBookmarkRepository analysisBookmarkRepository;
    private final AnalysisLikeRepository analysisLikeRepository;
    // private final UserScoreService userScoreService;
    private final CoinPriceService coinPriceService;

    /**
     * 분석글 생성
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "trendingAnalysis", "featuredAnalysis"}, allEntries = true)
    public CoinAnalysis createAnalysis(Long userId, String coinId, String coinName, String title, 
                                     String content, CoinAnalysis.AnalysisType analysisType,
                                     CoinAnalysis.PredictionPeriod predictionPeriod, 
                                     BigDecimal targetPrice, BigDecimal expectedReturnPercent,
                                     CoinAnalysis.InvestmentRecommendation recommendation,
                                     Integer riskLevel, Integer confidenceLevel, String tags) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 현재 가격 조회
        BigDecimal currentPrice = coinPriceService.getCurrentPrice(coinId);

        CoinAnalysis analysis = CoinAnalysis.builder()
                .user(user)
                .coinId(coinId)
                .coinName(coinName)
                .title(title)
                .content(content)
                .analysisType(analysisType)
                .predictionPeriod(predictionPeriod)
                .currentPrice(currentPrice)
                .targetPrice(targetPrice)
                .expectedReturnPercent(expectedReturnPercent)
                .recommendation(recommendation)
                .riskLevel(riskLevel)
                .confidenceLevel(confidenceLevel)
                .tags(tags)
                .viewCount(0)
                .likeCount(0)
                .bookmarkCount(0)
                .build();

        CoinAnalysis savedAnalysis = coinAnalysisRepository.save(analysis);

        // 사용자 점수 업데이트
        // int scorePoints = calculateAnalysisScore(analysisType, confidenceLevel, content.length());
        // userScoreService.addCommunityScore(userId, scorePoints);

        log.info("코인 분석글 생성 완료: userId={}, analysisId={}, coinId={}, type={}", 
                userId, savedAnalysis.getId(), coinId, analysisType);

        return savedAnalysis;
    }

    /**
     * 분석글 수정
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "analysisDetail"}, key = "#analysisId")
    public CoinAnalysis updateAnalysis(Long analysisId, Long userId, String title, String content,
                                     BigDecimal targetPrice, BigDecimal expectedReturnPercent,
                                     CoinAnalysis.InvestmentRecommendation recommendation,
                                     Integer riskLevel, Integer confidenceLevel, String tags) {
        
        CoinAnalysis analysis = getAnalysisById(analysisId);
        validateAnalysisOwnership(analysis, userId);

        analysis.setTitle(title);
        analysis.setContent(content);
        analysis.setTargetPrice(targetPrice);
        analysis.setExpectedReturnPercent(expectedReturnPercent);
        analysis.setRecommendation(recommendation);
        analysis.setRiskLevel(riskLevel);
        analysis.setConfidenceLevel(confidenceLevel);
        analysis.setTags(tags);

        CoinAnalysis updatedAnalysis = coinAnalysisRepository.save(analysis);
        log.info("분석글 수정 완료: analysisId={}, userId={}", analysisId, userId);

        return updatedAnalysis;
    }

    /**
     * 분석글 상세 조회 (조회수 증가)
     */
    @Transactional
    @Cacheable(value = "analysisDetail", key = "#analysisId")
    public CoinAnalysis getAnalysisDetail(Long analysisId, Long viewerUserId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        // 작성자가 아닌 경우에만 조회수 증가
        if (viewerUserId == null || !analysis.getUser().getId().equals(viewerUserId)) {
            analysis.incrementViewCount();
            coinAnalysisRepository.save(analysis);
        }

        return analysis;
    }

    /**
     * 분석글 기본 조회
     */
    public CoinAnalysis getAnalysisById(Long analysisId) {
        return coinAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("분석글을 찾을 수 없습니다."));
    }

    /**
     * 코인별 분석글 목록 조회
     */
    @Cacheable(value = "coinAnalysis", key = "#coinId + '-' + #pageable.pageNumber")
    public Page<CoinAnalysis> getAnalysisByCoin(String coinId, Pageable pageable) {
        return coinAnalysisRepository.findByCoinIdOrderByCreatedAtDesc(coinId, pageable);
    }

    /**
     * 사용자별 분석글 목록 조회
     */
    public Page<CoinAnalysis> getAnalysisByUser(Long userId, Pageable pageable) {
        return coinAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 인기 분석글 조회
     */
    @Cacheable(value = "popularAnalysis")
    public Page<CoinAnalysis> getPopularAnalysis(Pageable pageable) {
        return coinAnalysisRepository.findByOrderByLikeCountDescCreatedAtDesc(pageable);
    }

    /**
     * 추천 분석글 조회
     */
    @Cacheable(value = "featuredAnalysis")
    public Page<CoinAnalysis> getFeaturedAnalysis(Pageable pageable) {
        return coinAnalysisRepository.findByIsFeaturedTrueOrderByCreatedAtDesc(pageable);
    }

    /**
     * 검증된 분석글 조회
     */
    public Page<CoinAnalysis> getVerifiedAnalysis(Pageable pageable) {
        return coinAnalysisRepository.findByIsVerifiedTrueOrderByAccuracyScoreDesc(pageable);
    }

    /**
     * 트렌딩 분석글 조회 (최근 1주일)
     */
    @Cacheable(value = "trendingAnalysis")
    public List<CoinAnalysis> getTrendingAnalysis(int limit) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        Pageable pageable = PageRequest.of(0, limit);
        return coinAnalysisRepository.findTrendingAnalysis(oneWeekAgo, pageable);
    }

    /**
     * 분석 유형별 조회
     */
    public Page<CoinAnalysis> getAnalysisByType(CoinAnalysis.AnalysisType analysisType, Pageable pageable) {
        return coinAnalysisRepository.findByAnalysisTypeOrderByCreatedAtDesc(analysisType, pageable);
    }

    /**
     * 투자 추천도별 조회
     */
    public Page<CoinAnalysis> getAnalysisByRecommendation(CoinAnalysis.InvestmentRecommendation recommendation, Pageable pageable) {
        return coinAnalysisRepository.findByRecommendationOrderByCreatedAtDesc(recommendation, pageable);
    }

    /**
     * 분석글 검색
     */
    public Page<CoinAnalysis> searchAnalysis(String keyword, Pageable pageable) {
        return coinAnalysisRepository.findByTitleContainingOrContentContaining(keyword, pageable);
    }

    /**
     * 분석글 좋아요 토글
     */
    @Transactional
    @CacheEvict(value = {"analysisDetail", "popularAnalysis"}, key = "#analysisId")
    public boolean toggleLike(Long analysisId, Long userId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        // 실제 구현에서는 별도의 AnalysisLike 테이블을 만들어 관리
        // 여기서는 간단한 로직으로 구현
        boolean isLiked = checkIfUserLiked(userId, analysisId);
        
        if (isLiked) {
            analysis.decrementLikeCount();
            removeLike(userId, analysisId);
        } else {
            analysis.incrementLikeCount();
            addLike(userId, analysisId);
            
            // 분석글 작성자에게 커뮤니티 점수 추가
            // userScoreService.addCommunityScore(analysis.getUser().getId(), 2);
        }

        coinAnalysisRepository.save(analysis);
        return !isLiked; // 새로운 좋아요 상태 반환
    }

    /**
     * 분석글 북마크 토글
     */
    @Transactional
    @CacheEvict(value = "analysisDetail", key = "#analysisId")
    public boolean toggleBookmark(Long analysisId, Long userId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        boolean isBookmarked = checkIfUserBookmarked(userId, analysisId);
        
        if (isBookmarked) {
            analysis.decrementBookmarkCount();
            removeBookmark(userId, analysisId);
        } else {
            analysis.incrementBookmarkCount();
            addBookmark(userId, analysisId);
        }

        coinAnalysisRepository.save(analysis);
        return !isBookmarked;
    }

    /**
     * 분석글 추천 설정 (관리자)
     */
    @Transactional
    @CacheEvict(value = "featuredAnalysis", allEntries = true)
    public void setFeatured(Long analysisId, boolean featured) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        analysis.setFeatured(featured);
        coinAnalysisRepository.save(analysis);
        
        if (featured) {
            // 추천 분석글 선정 보너스
            // userScoreService.addCommunityScore(analysis.getUser().getId(), 100);
        }
        
        log.info("분석글 추천 설정: analysisId={}, featured={}", analysisId, featured);
    }

    /**
     * 분석글 검증 설정 (관리자)
     */
    @Transactional
    public void setVerified(Long analysisId, boolean verified) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        analysis.setVerified(verified);
        coinAnalysisRepository.save(analysis);
        
        if (verified) {
            // 검증된 분석글 보너스
            // userScoreService.addCommunityScore(analysis.getUser().getId(), 150);
        }
        
        log.info("분석글 검증 설정: analysisId={}, verified={}", analysisId, verified);
    }

    /**
     * 예측 정확도 업데이트 (배치 작업)
     */
    @Transactional
    public void updatePredictionAccuracy(Long analysisId, BigDecimal actualPrice) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        analysis.calculateAccuracy(actualPrice);
        coinAnalysisRepository.save(analysis);
        
        // 높은 정확도에 대한 보상
        if (analysis.getAccuracyScore() != null && 
            analysis.getAccuracyScore().compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            
            // int bonusPoints = analysis.getAccuracyScore().compareTo(BigDecimal.valueOf(0.9)) >= 0 ? 100 : 50;
            // userScoreService.addPredictionScore(analysis.getUser().getId(), bonusPoints, analysis.getAccuracyScore());
        }
        
        log.info("예측 정확도 업데이트: analysisId={}, accuracy={}", 
                analysisId, analysis.getAccuracyScore());
    }

    /**
     * 분석글 통계 조회
     */
    public Map<String, Object> getAnalysisStatistics(Long analysisId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("viewCount", analysis.getViewCount());
        stats.put("likeCount", analysis.getLikeCount());
        stats.put("bookmarkCount", analysis.getBookmarkCount());
        stats.put("accuracyScore", analysis.getAccuracyScore());
        stats.put("isFeatured", analysis.isFeatured());
        stats.put("isVerified", analysis.isVerified());
        
        return stats;
    }

    /**
     * 사용자별 분석 통계
     */
    public CoinAnalysisDto.Statistics getUserAnalysisStatistics(Long userId) {
        // 기본 통계 조회
        Long totalAnalyses = coinAnalysisRepository.countByUserId(userId);
        
        // 정확도 관련 통계
        List<CoinAnalysis> userAnalyses = coinAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        
        long successfulPredictions = userAnalyses.stream()
                .filter(analysis -> analysis.getAccuracyScore() != null && 
                        analysis.getAccuracyScore().compareTo(BigDecimal.valueOf(60)) >= 0)
                .count();
        
        long failedPredictions = userAnalyses.stream()
                .filter(analysis -> analysis.getAccuracyScore() != null && 
                        analysis.getAccuracyScore().compareTo(BigDecimal.valueOf(60)) < 0)
                .count();
        
        // 진행 중인 예측 수 (정확도가 아직 계산되지 않은 분석)
        long activePredictions = userAnalyses.stream()
                .filter(analysis -> analysis.getAccuracyScore() == null)
                .count();
        
        // 평균 정확도 계산
        BigDecimal averageAccuracy = userAnalyses.stream()
                .filter(analysis -> analysis.getAccuracyScore() != null)
                .map(CoinAnalysis::getAccuracyScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long validAnalysesCount = userAnalyses.stream()
                .filter(analysis -> analysis.getAccuracyScore() != null)
                .count();
        
        if (validAnalysesCount > 0) {
            averageAccuracy = averageAccuracy.divide(BigDecimal.valueOf(validAnalysesCount), 2, java.math.RoundingMode.HALF_UP);
        }
        
        return CoinAnalysisDto.Statistics.builder()
                .totalAnalyses(totalAnalyses)
                .averageAccuracy(averageAccuracy)
                .successfulPredictions(successfulPredictions)
                .failedPredictions(failedPredictions)
                .activePredictions(activePredictions)
                .build();
    }
    
    /**
     * 분석 ID로 응답 DTO 조회
     */
    public CoinAnalysisDto.Response getAnalysisResponseById(Long analysisId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        return convertToResponseDto(analysis);
    }

    /**
     * 코인별 분석 통계
     */
    @Cacheable(value = "coinAnalysisStats")
    public List<Object[]> getCoinAnalysisStatistics() {
        return coinAnalysisRepository.findAnalysisStatsByCoin();
    }

    /**
     * 분석글 삭제
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "analysisDetail", "trendingAnalysis"}, allEntries = true)
    public void deleteAnalysis(Long analysisId, Long userId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        validateAnalysisOwnership(analysis, userId);
        
        coinAnalysisRepository.delete(analysis);
        log.info("분석글 삭제 완료: analysisId={}, userId={}", analysisId, userId);
    }

    // Private helper methods

    private void validateAnalysisOwnership(CoinAnalysis analysis, Long userId) {
        if (!analysis.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("분석글에 대한 권한이 없습니다.");
        }
    }

    private boolean checkIfUserLiked(Long userId, Long analysisId) {
        return analysisLikeRepository.existsByUserIdAndAnalysisId(userId, analysisId);
    }

    private boolean checkIfUserBookmarked(Long userId, Long analysisId) {
        return analysisBookmarkRepository.existsByUserIdAndAnalysisId(userId, analysisId);
    }

    private void addLike(Long userId, Long analysisId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        AnalysisLike like = AnalysisLike.builder()
                .user(user)
                .analysis(analysis)
                .build();
        
        analysisLikeRepository.save(like);
    }

    private void removeLike(Long userId, Long analysisId) {
        analysisLikeRepository.deleteByUserIdAndAnalysisId(userId, analysisId);
    }

    private void addBookmark(Long userId, Long analysisId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        CoinAnalysis analysis = getAnalysisById(analysisId);
        
        AnalysisBookmark bookmark = AnalysisBookmark.builder()
                .user(user)
                .analysis(analysis)
                .build();
        
        analysisBookmarkRepository.save(bookmark);
    }

    private void removeBookmark(Long userId, Long analysisId) {
        analysisBookmarkRepository.deleteByUserIdAndAnalysisId(userId, analysisId);
    }

    /**
     * DTO 기반: 분석글 목록 조회 (페이징 처리)
     */
    public PageResponse<CoinAnalysisDto.Summary> getAnalysisList(Pageable pageable) {
        Page<CoinAnalysis> analysisPage = coinAnalysisRepository.findAll(pageable);
        
        List<CoinAnalysisDto.Summary> analysisDtos = analysisPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(analysisDtos, pageable, analysisPage.getTotalElements());
    }

    /**
     * DTO 기반: 특정 분석글 조회
     */
    public CoinAnalysisDto.Response getAnalysisDto(Long analysisId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        return convertToResponseDto(analysis);
    }

    // DTO 변환 메서드들

    private CoinAnalysisDto.Response convertToResponseDto(CoinAnalysis analysis) {
        return CoinAnalysisDto.Response.builder()
                .id(analysis.getId())
                .coinSymbol(analysis.getCoinId())
                .coinName(analysis.getCoinName())
                .title(analysis.getTitle())
                .content(analysis.getContent())
                .analysisType(analysis.getAnalysisType())
                .confidenceLevel(analysis.getConfidenceLevel())
                .analysisPriceAtTime(analysis.getCurrentPrice())
                .currentPrice(analysis.getCurrentPrice())
                .targetPrice(analysis.getTargetPrice())
                .accuracyScore(analysis.getAccuracyScore())
                .viewCount(analysis.getViewCount().longValue())
                .likeCount(analysis.getLikeCount().longValue())
                .tags(analysis.getTags() != null ? List.of(analysis.getTags().split(",")) : List.of())
                .createdAt(analysis.getCreatedAt())
                .updatedAt(analysis.getUpdatedAt())
                .build();
    }

    private CoinAnalysisDto.Summary convertToSummaryDto(CoinAnalysis analysis) {
        return CoinAnalysisDto.Summary.builder()
                .id(analysis.getId())
                .coinSymbol(analysis.getCoinId())
                .title(analysis.getTitle())
                .analysisType(analysis.getAnalysisType())
                .confidenceLevel(analysis.getConfidenceLevel())
                .accuracyScore(analysis.getAccuracyScore())
                .likeCount(analysis.getLikeCount().longValue())
                .authorNickname(analysis.getUser().getUsername())
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    // 추가 DTO 기반 메서드들 (컨트롤러에서 호출)

    /**
     * DTO 기반: 분석글 생성
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "trendingAnalysis", "featuredAnalysis"}, allEntries = true)
    public CoinAnalysisDto.Response createAnalysis(String username, CoinAnalysisDto.CreateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        CoinAnalysis analysis = CoinAnalysis.builder()
                .user(user)
                .coinId(request.getCoinSymbol())
                .coinName(request.getCoinSymbol()) // 실제로는 코인 이름을 별도로 조회
                .title(request.getTitle())
                .content(request.getContent())
                .analysisType(request.getAnalysisType())
                .predictionPeriod(CoinAnalysis.PredictionPeriod.SHORT_TERM) // 기본값
                .confidenceLevel(request.getConfidenceLevel())
                .targetPrice(request.getTargetPrice())
                .recommendation(CoinAnalysis.InvestmentRecommendation.HOLD) // 기본값
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .viewCount(0)
                .likeCount(0)
                .bookmarkCount(0)
                .build();

        CoinAnalysis savedAnalysis = coinAnalysisRepository.save(analysis);
        
        log.info("코인 분석글 생성 완료: username={}, analysisId={}, coinSymbol={}", 
                username, savedAnalysis.getId(), request.getCoinSymbol());

        return convertToResponseDto(savedAnalysis);
    }

    /**
     * DTO 기반: 분석글 목록 조회 (필터링)
     */
    public PageResponse<CoinAnalysisDto.Summary> getAnalyses(CoinAnalysisDto.FilterRequest filter, Pageable pageable) {
        Page<CoinAnalysis> analysisPage = coinAnalysisRepository.findAll(pageable);
        
        List<CoinAnalysisDto.Summary> summaries = analysisPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(summaries, pageable, analysisPage.getTotalElements());
    }

    /**
     * DTO 기반: 사용자 분석글 목록 조회
     */
    public PageResponse<CoinAnalysisDto.Summary> getUserAnalyses(String username, CoinAnalysisDto.FilterRequest filter, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        Page<CoinAnalysis> analysisPage = coinAnalysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        
        List<CoinAnalysisDto.Summary> summaries = analysisPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(summaries, pageable, analysisPage.getTotalElements());
    }

    /**
     * DTO 기반: 분석글 상세 조회
     */
    public CoinAnalysisDto.Response getAnalysisDetails(Long analysisId) {
        CoinAnalysis analysis = getAnalysisById(analysisId);
        analysis.incrementViewCount();
        coinAnalysisRepository.save(analysis);
        
        return convertToResponseDto(analysis);
    }

    /**
     * DTO 기반: 분석글 수정
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "analysisDetail"}, key = "#analysisId")
    public CoinAnalysisDto.Response updateAnalysis(Long analysisId, String username, CoinAnalysisDto.UpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        CoinAnalysis analysis = getAnalysisById(analysisId);
        validateAnalysisOwnership(analysis, user.getId());

        if (request.getTitle() != null) {
            analysis.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            analysis.setContent(request.getContent());
        }
        if (request.getConfidenceLevel() != null) {
            analysis.setConfidenceLevel(request.getConfidenceLevel());
        }

        CoinAnalysis updatedAnalysis = coinAnalysisRepository.save(analysis);
        
        log.info("분석글 수정 완료: analysisId={}, username={}", analysisId, username);
        
        return convertToResponseDto(updatedAnalysis);
    }

    /**
     * DTO 기반: 분석글 삭제
     */
    @Transactional
    @CacheEvict(value = {"coinAnalysis", "analysisDetail", "trendingAnalysis"}, allEntries = true)
    public void deleteAnalysis(Long analysisId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        CoinAnalysis analysis = getAnalysisById(analysisId);
        validateAnalysisOwnership(analysis, user.getId());
        
        coinAnalysisRepository.delete(analysis);
        log.info("분석글 삭제 완료: analysisId={}, username={}", analysisId, username);
    }

    /**
     * 분석글 좋아요 토글 (username 기반)
     */
    @Transactional
    public Boolean toggleAnalysisLike(Long analysisId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return toggleLike(analysisId, user.getId());
    }

    /**
     * 특정 코인의 최신 분석글 조회
     */
    @Cacheable(value = "latestAnalysesByCoin", key = "#coinSymbol + '_' + #limit + '_' + #sortBy")
    public List<CoinAnalysisDto.Summary> getLatestAnalysesByCoin(String coinSymbol, Integer limit, String sortBy) {
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 10, 
                Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt"));
        
        // 임시 구현: 기본 findAll 사용
        Page<CoinAnalysis> analysisPage = coinAnalysisRepository.findAll(pageable);
        return analysisPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 인기 분석글 조회
     */
    @Cacheable(value = "popularAnalyses", key = "#days + '_' + #limit + '_' + #sortBy")
    public List<CoinAnalysisDto.Summary> getPopularAnalyses(Integer days, Integer limit, String sortBy) {
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 10, 
                Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "likeCount"));
        
        // 임시 구현: 기본 findAll 사용
        Page<CoinAnalysis> analysisPage = coinAnalysisRepository.findAll(pageable);
        return analysisPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 전체 분석글 통계 조회
     */
    @Cacheable(value = "analysisStatistics", key = "#coinSymbol + '_' + #days")
    public CoinAnalysisDto.Statistics getOverallAnalysisStatistics(String coinSymbol, Integer days) {
        // 임시 구현: 기본값 반환
        return CoinAnalysisDto.Statistics.builder()
                .totalAnalyses(0L)
                .averageAccuracy(BigDecimal.ZERO)
                .successfulPredictions(0L)
                .failedPredictions(0L)
                .activePredictions(0L)
                .build();
    }

    /**
     * 전문가 분석글 조회
     */
    @Cacheable(value = "expertAnalyses")
    public PageResponse<CoinAnalysisDto.Summary> getExpertAnalyses(CoinAnalysisDto.FilterRequest filter, Pageable pageable) {
        // 임시 구현: 기본 findAll 사용
        Page<CoinAnalysis> analyses = coinAnalysisRepository.findAll(pageable);
        
        List<CoinAnalysisDto.Summary> summaries = analyses.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(summaries, pageable, analyses.getTotalElements());
    }

    /**
     * 인기 분석 태그 조회
     */
    @Cacheable(value = "popularAnalysisTags", key = "#coinSymbol + '_' + #limit")
    public List<String> getPopularAnalysisTags(String coinSymbol, Integer limit) {
        // 임시 구현: 빈 리스트 반환
        return List.of();
    }
}
