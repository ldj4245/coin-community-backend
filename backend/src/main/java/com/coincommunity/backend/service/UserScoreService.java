package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.UserScoreDto;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.entity.UserScore;
import com.coincommunity.backend.entity.CoinAnalysis;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.repository.UserScoreRepository;
import com.coincommunity.backend.repository.CoinAnalysisRepository;
import com.coincommunity.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 사용자 점수 및 레벨 시스템 서비스
 * 30년차 베테랑 개발자 아키텍처 적용:
 * - 실시간 점수 계산 시스템
 * - 다단계 레벨링 알고리즘
 * - 성취도 기반 배지 시스템
 * - 정확도 기반 전문가 인증
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserScoreService {

    private final UserRepository userRepository;
    private final UserScoreRepository userScoreRepository;
    private final CoinAnalysisRepository coinAnalysisRepository;
    private final PostRepository postRepository;

    // 점수 계산 상수
    private static final int ANALYSIS_BASE_SCORE = 10;
    private static final int POST_BASE_SCORE = 5;
    private static final int COMMENT_BASE_SCORE = 2;
    private static final int LIKE_RECEIVED_SCORE = 1;
    private static final double ACCURACY_MULTIPLIER = 2.0;
    private static final int EXPERT_THRESHOLD_SCORE = 5000;
    private static final double EXPERT_ACCURACY_THRESHOLD = 80.0;

    // 레벨 시스템 구성
    private static final int[] LEVEL_THRESHOLDS = {
        0, 100, 300, 600, 1000, 1500, 2500, 4000, 6000, 9000, 15000
    };
    
    private static final String[] LEVEL_NAMES = {
        "신규", "초보자", "견습생", "중급자", "숙련자", "전문가", "고급자", "전문가", "마스터", "그랜드마스터", "레전드"
    };

    /**
     * 사용자 점수 정보 조회
     */
    @Transactional(readOnly = true)
    public UserScoreDto.Response getUserScore(String username) {
        log.info("사용자 점수 정보 조회: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
            
        UserScore userScore = userScoreRepository.findByUserId(user.getId())
            .orElseGet(() -> createNewUserScore(user));
            
        // 실시간 점수 재계산
        updateUserScoreRealtime(user, userScore);
        
        int level = calculateLevel(userScore.getTotalScore());
        String levelName = LEVEL_NAMES[Math.min(level, LEVEL_NAMES.length - 1)];
        int pointsToNext = calculatePointsToNextLevel(userScore.getTotalScore());
        double progress = calculateLevelProgress(userScore.getTotalScore());
        
        return UserScoreDto.Response.builder()
                .userId(user.getId())
                .userNickname(user.getNickname())
                .totalScore(userScore.getTotalScore())
                .level(level)
                .levelName(levelName)
                .pointsToNextLevel(pointsToNext)
                .progressPercentage(progress)
                .lastUpdated(userScore.getUpdatedAt())
                .build();
    }

    /**
     * 사용자 점수 업데이트
     */
    @Transactional
    public UserScoreDto.Response updateUserScore(String username, UserScoreDto.UpdateRequest request) {
        log.info("사용자 점수 업데이트: {}, 추가점수: {}", username, request.getAdditionalScore());
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
            
        UserScore userScore = userScoreRepository.findByUserId(user.getId())
            .orElseGet(() -> createNewUserScore(user));
        
        // 점수 업데이트
        int newTotalScore = userScore.getTotalScore() + request.getAdditionalScore();
        userScore.setTotalScore(Math.max(0, newTotalScore)); // 음수 방지
        
        // 활동별 점수 업데이트
        switch (request.getActivityType()) {
            case "ANALYSIS":
                userScore.setAnalysisScore(userScore.getAnalysisScore() + request.getAdditionalScore());
                break;
            case "COMMUNITY":
                userScore.setCommunityScore(userScore.getCommunityScore() + request.getAdditionalScore());
                break;
            case "TRADING":
                userScore.setTradingScore(userScore.getTradingScore() + request.getAdditionalScore());
                break;
        }
        
        // 전문가 상태 업데이트
        updateExpertStatus(userScore);
        
        userScoreRepository.save(userScore);
        
        return getUserScore(username);
    }

    /**
     * 사용자 랭킹 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<UserScoreDto.Ranking> getUserRanking(UserScoreDto.RankingFilter filter, Pageable pageable) {
        log.info("사용자 랭킹 조회: {}", filter);
        
        Page<UserScore> userScorePage = userScoreRepository.findAllByOrderByTotalScoreDesc(pageable);
        
        List<UserScoreDto.Ranking> rankings = userScorePage.getContent().stream()
            .map(this::convertToRanking)
            .collect(Collectors.toList());
            
        // 순위 계산
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank((long)(pageable.getPageNumber() * pageable.getPageSize() + i + 1));
        }
        
        return PageResponse.of(rankings, pageable, userScorePage.getTotalElements());
    }

    /**
     * 사용자 랭킹 위치 조회
     */
    @Transactional(readOnly = true)
    public UserScoreDto.Ranking getUserRankingPosition(String username, String rankingType) {
        log.info("사용자 랭킹 위치 조회: {}, {}", username, rankingType);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        
        UserScore userScore = userScoreRepository.findByUserId(user.getId())
            .orElseGet(() -> createNewUserScore(user));
        
        // 현재 사용자보다 높은 점수를 가진 사용자 수를 계산하여 순위 결정
        long higherScoreCount = userScoreRepository.countByTotalScoreGreaterThan(userScore.getTotalScore());
        
        return convertToRanking(userScore).toBuilder()
            .rank(Long.valueOf(higherScoreCount + 1))
            .build();
    }

    /**
     * 상위 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserScoreDto.TopUser> getTopUsers(int limit) {
        log.info("상위 사용자 목록 조회: {}", limit);
        
        List<UserScore> topUserScores = userScoreRepository.findTop10ByOrderByTotalScoreDesc();
        
        return topUserScores.stream()
            .limit(limit)
            .map(this::convertToTopUser)
            .collect(Collectors.toList());
    }

    /**
     * 레벨 시스템 정보 조회
     */
    public List<UserScoreDto.LevelInfo> getLevelSystem() {
        log.info("레벨 시스템 정보 조회");
        
        return IntStream.range(0, LEVEL_NAMES.length)
            .mapToObj(i -> {
                int minScore = i < LEVEL_THRESHOLDS.length ? LEVEL_THRESHOLDS[i] : LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
                int maxScore = i + 1 < LEVEL_THRESHOLDS.length ? LEVEL_THRESHOLDS[i + 1] - 1 : Integer.MAX_VALUE;
                
                return UserScoreDto.LevelInfo.builder()
                    .level(i)
                    .levelName(LEVEL_NAMES[i])
                    .minScore(minScore)
                    .maxScore(maxScore)
                    .userCount(getUserCountForLevel(i))
                    .description(getLevelDescription(i))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 사용자 성취도 조회
     */
    @Transactional(readOnly = true)
    public List<UserScoreDto.Achievement> getUserAchievements(String username) {
        log.info("사용자 성취도 조회: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
            
        UserScore userScore = userScoreRepository.findByUserId(user.getId())
            .orElseGet(() -> createNewUserScore(user));
        
        List<UserScoreDto.Achievement> achievements = new ArrayList<>();
        
        // 분석 관련 성취도
        long analysisCount = coinAnalysisRepository.countByUserId(user.getId());
        if (analysisCount >= 10) {
            achievements.add(createAchievement("분석 전문가", "10개 이상의 분석글 작성", true));
        }
        if (analysisCount >= 50) {
            achievements.add(createAchievement("분석 마스터", "50개 이상의 분석글 작성", true));
        }
        
        // 점수 관련 성취도
        if (userScore.getTotalScore() >= 1000) {
            achievements.add(createAchievement("천점 돌파", "총 점수 1000점 달성", true));
        }
        if (userScore.getTotalScore() >= 5000) {
            achievements.add(createAchievement("오천점 달성", "총 점수 5000점 달성", true));
        }
        
        // 전문가 인증
        if (userScore.isExpert()) {
            achievements.add(createAchievement("전문가 인증", "전문가 자격 인증 완료", true));
        }
        
        return achievements;
    }

    /**
     * 점수 히스토리 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<UserScoreDto.ScoreHistory> getScoreHistory(String username, Pageable pageable) {
        log.info("점수 히스토리 조회: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        
        List<UserScoreDto.ScoreHistory> histories = new ArrayList<>();
        
        UserScore userScore = userScoreRepository.findByUserId(user.getId()).orElse(null);
        if (userScore != null) {
            histories.add(UserScoreDto.ScoreHistory.builder()
                .date(userScore.getUpdatedAt())
                .totalScore(userScore.getTotalScore())
                .scoreChange(0) // 실제로는 이전 점수와의 차이 계산
                .reason("현재 점수")
                .activityType("CURRENT")
                .build());
        }
        
        return PageResponse.of(histories, pageable, (long) histories.size());
    }

    /**
     * 활동 통계 조회
     */
    @Transactional(readOnly = true)
    public UserScoreDto.ActivityStats getActivityStats(String username) {
        log.info("활동 통계 조회: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        
        long analysisCount = coinAnalysisRepository.countByUserId(user.getId());
        long postCount = postRepository.countByUserId(user.getId());
        
        return UserScoreDto.ActivityStats.builder()
            .totalAnalysis((int) analysisCount)
            .totalPosts(postCount)
            .totalComments(0L) // Comment 엔티티가 있다면 계산
            .totalLikesReceived(0L) // Like 엔티티가 있다면 계산
            .averageAccuracy(calculateUserAccuracy(user.getId()))
            .build();
    }

    /**
     * 배지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserScoreDto.Badge> getBadges(String username) {
        log.info("배지 목록 조회: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        
        List<UserScoreDto.Badge> badges = new ArrayList<>();
        
        UserScore userScore = userScoreRepository.findByUserId(user.getId()).orElse(null);
        if (userScore != null && userScore.isExpert()) {
            badges.add(UserScoreDto.Badge.builder()
                .badgeId(1L)
                .badgeName("전문가")
                .description("전문가 인증을 받은 사용자")
                .iconUrl("/badges/expert.png")
                .earnedDate(userScore.getUpdatedAt())
                .rarity("RARE")
                .build());
        }
        
        long analysisCount = coinAnalysisRepository.countByUserId(user.getId());
        if (analysisCount >= 10) {
            badges.add(UserScoreDto.Badge.builder()
                .badgeId(2L)
                .badgeName("분석 전문가")
                .description("10개 이상의 분석글 작성")
                .iconUrl("/badges/analyst.png")
                .earnedDate(LocalDateTime.now())
                .rarity("COMMON")
                .build());
        }
        
        return badges;
    }

    /**
     * 분석 점수 계산
     */
    @Transactional
    public int calculateAnalysisScore(Long analysisId) {
        log.info("분석 점수 계산: {}", analysisId);
        
        CoinAnalysis analysis = coinAnalysisRepository.findById(analysisId)
            .orElseThrow(() -> new IllegalArgumentException("분석을 찾을 수 없습니다: " + analysisId));
        
        int baseScore = ANALYSIS_BASE_SCORE;
        int likeBonus = analysis.getLikeCount() * LIKE_RECEIVED_SCORE;
        
        // 정확도 보너스
        double accuracyBonus = 0;
        if (analysis.getAccuracyScore() != null) {
            accuracyBonus = analysis.getAccuracyScore().doubleValue() * ACCURACY_MULTIPLIER;
        }
        
        return (int) (baseScore + likeBonus + accuracyBonus);
    }

    /**
     * 커뮤니티 점수 계산
     */
    @Transactional
    public int calculateCommunityScore(Long postId) {
        log.info("커뮤니티 점수 계산: {}", postId);
        
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
        
        int baseScore = POST_BASE_SCORE;
        int likeBonus = post.getLikeCount() * LIKE_RECEIVED_SCORE;
        int viewBonus = post.getViewCount() / 10; // 조회수 10당 1점
        
        return baseScore + likeBonus + viewBonus;
    }

    /**
     * 거래 점수 계산
     */
    @Transactional
    public int calculateTradingScore(Long userId) {
        log.info("거래 점수 계산: {}", userId);
        
        // 실제로는 Transaction 엔티티를 통해 거래 성과를 분석
        // 현재는 기본 점수만 반환
        return 50; // 기본 거래 점수
    }

    /**
     * 리더보드 조회
     */
    @Transactional(readOnly = true)
    public List<UserScoreDto.LeaderboardEntry> getLeaderboard(String period, int limit) {
        log.info("리더보드 조회: {}, {}", period, limit);
        
        List<UserScore> topUsers = userScoreRepository.findTop10ByOrderByTotalScoreDesc();
        
        return topUsers.stream()
            .limit(limit)
            .map(userScore -> {
                User user = userScore.getUser();
                int level = calculateLevel(userScore.getTotalScore());
                return UserScoreDto.LeaderboardEntry.builder()
                    .rank(1) // 실제로는 순위 계산 필요
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .totalScore(userScore.getTotalScore())
                    .level(level)
                    .levelName(LEVEL_NAMES[Math.min(level, LEVEL_NAMES.length - 1)])
                    .isExpert(userScore.isExpert())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 전문가 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserScoreDto.ExpertUser> getExpertUsers(Pageable pageable) {
        log.info("전문가 사용자 목록 조회");
        
        Page<UserScore> expertUsers = userScoreRepository.findByIsExpertTrue(pageable);
        
        return expertUsers.getContent().stream()
            .map(userScore -> {
                User user = userScore.getUser();
                return UserScoreDto.ExpertUser.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .totalScore(userScore.getTotalScore())
                    .level(calculateLevel(userScore.getTotalScore()))
                    .expertSince(userScore.getUpdatedAt())
                    .analysisCount((int) coinAnalysisRepository.countByUserId(user.getId()))
                    .averageAccuracy(calculateUserAccuracy(user.getId()))
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
            })
            .collect(Collectors.toList());
    }

    // === 헬퍼 메서드들 ===

    private UserScore createNewUserScore(User user) {
        UserScore userScore = UserScore.builder()
            .user(user)
            .totalScore(0)
            .analysisScore(0)
            .communityScore(0)
            .tradingScore(0)
            .isExpert(false)
            .build();
        return userScoreRepository.save(userScore);
    }

    private void updateUserScoreRealtime(User user, UserScore userScore) {
        // 실시간 점수 재계산 로직
        int analysisScore = calculateTotalAnalysisScore(user.getId());
        int communityScore = calculateTotalCommunityScore(user.getId());
        int tradingScore = calculateTotalTradingScore(user.getId());
        
        userScore.setAnalysisScore(analysisScore);
        userScore.setCommunityScore(communityScore);
        userScore.setTradingScore(tradingScore);
        userScore.setTotalScore(analysisScore + communityScore + tradingScore);
        
        updateExpertStatus(userScore);
        userScoreRepository.save(userScore);
    }

    private void updateExpertStatus(UserScore userScore) {
        boolean isExpert = userScore.getTotalScore() >= EXPERT_THRESHOLD_SCORE &&
                          calculateUserAccuracy(userScore.getUser().getId()) >= EXPERT_ACCURACY_THRESHOLD;
        userScore.setExpert(isExpert);
    }

    private int calculateLevel(int totalScore) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalScore >= LEVEL_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    private int calculatePointsToNextLevel(int totalScore) {
        int currentLevel = calculateLevel(totalScore);
        if (currentLevel >= LEVEL_THRESHOLDS.length - 1) {
            return 0; // 최고 레벨
        }
        return LEVEL_THRESHOLDS[currentLevel + 1] - totalScore;
    }

    private double calculateLevelProgress(int totalScore) {
        int currentLevel = calculateLevel(totalScore);
        if (currentLevel >= LEVEL_THRESHOLDS.length - 1) {
            return 100.0; // 최고 레벨
        }
        
        int currentLevelScore = LEVEL_THRESHOLDS[currentLevel];
        int nextLevelScore = LEVEL_THRESHOLDS[currentLevel + 1];
        int progressScore = totalScore - currentLevelScore;
        int requiredScore = nextLevelScore - currentLevelScore;
        
        return (double) progressScore / requiredScore * 100.0;
    }

    private UserScoreDto.Ranking convertToRanking(UserScore userScore) {
        User user = userScore.getUser();
        int level = calculateLevel(userScore.getTotalScore());
        
        return UserScoreDto.Ranking.builder()
            .username(user.getUsername())
            .nickname(user.getNickname())
            .totalScore(userScore.getTotalScore())
            .level(level)
            .levelName(LEVEL_NAMES[Math.min(level, LEVEL_NAMES.length - 1)])
            .isExpert(userScore.isExpert())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }

    private UserScoreDto.TopUser convertToTopUser(UserScore userScore) {
        User user = userScore.getUser();
        int level = calculateLevel(userScore.getTotalScore());
        
        return UserScoreDto.TopUser.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .totalScore(userScore.getTotalScore())
            .level(level)
            .levelName(LEVEL_NAMES[Math.min(level, LEVEL_NAMES.length - 1)])
            .isExpert(userScore.isExpert())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }

    private UserScoreDto.Achievement createAchievement(String title, String description, boolean unlocked) {
        return UserScoreDto.Achievement.builder()
            .achievementId(System.currentTimeMillis()) // 임시 ID
            .title(title)
            .description(description)
            .unlocked(unlocked)
            .unlockedDate(unlocked ? LocalDateTime.now() : null)
            .iconUrl("/achievements/" + title.toLowerCase().replace(" ", "_") + ".png")
            .build();
    }

    private double calculateUserAccuracy(Long userId) {
        List<CoinAnalysis> analyses = coinAnalysisRepository.findByUserId(userId);
        if (analyses.isEmpty()) {
            return 0.0;
        }
        
        double totalAccuracy = analyses.stream()
            .filter(analysis -> analysis.getAccuracyScore() != null)
            .mapToDouble(analysis -> analysis.getAccuracyScore().doubleValue())
            .average()
            .orElse(0.0);
            
        return Math.round(totalAccuracy * 100.0) / 100.0;
    }

    private int calculateTotalAnalysisScore(Long userId) {
        List<CoinAnalysis> analyses = coinAnalysisRepository.findByUserId(userId);
        return analyses.stream()
            .mapToInt(this::calculateSingleAnalysisScore)
            .sum();
    }

    private int calculateSingleAnalysisScore(CoinAnalysis analysis) {
        int baseScore = ANALYSIS_BASE_SCORE;
        int likeBonus = analysis.getLikeCount() * LIKE_RECEIVED_SCORE;
        double accuracyBonus = analysis.getAccuracyScore() != null ? 
            analysis.getAccuracyScore().doubleValue() * ACCURACY_MULTIPLIER : 0;
        return (int) (baseScore + likeBonus + accuracyBonus);
    }

    private int calculateTotalCommunityScore(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);
        return posts.stream()
            .mapToInt(this::calculateSinglePostScore)
            .sum();
    }

    private int calculateSinglePostScore(Post post) {
        int baseScore = POST_BASE_SCORE;
        int likeBonus = post.getLikeCount() * LIKE_RECEIVED_SCORE;
        int viewBonus = post.getViewCount() / 10;
        return baseScore + likeBonus + viewBonus;
    }

    private int calculateTotalTradingScore(Long userId) {
        // 실제로는 Transaction 엔티티를 통해 계산
        return 0;
    }

    private int getUserCountForLevel(int level) {
        // 실제로는 데이터베이스에서 해당 레벨 사용자 수를 조회
        return (int) (Math.random() * 100);
    }

    private String getLevelDescription(int level) {
        switch (level) {
            case 0: return "커뮤니티에 새로 가입한 사용자";
            case 1: return "기본적인 활동을 시작한 사용자";
            case 2: return "분석과 커뮤니티 활동에 참여하는 사용자";
            case 3: return "꾸준한 활동으로 경험을 쌓은 사용자";
            case 4: return "풍부한 경험과 지식을 보유한 사용자";
            case 5: return "전문적인 분석 능력을 인정받은 사용자";
            default: return "최고 수준의 전문가";
        }
    }
}
