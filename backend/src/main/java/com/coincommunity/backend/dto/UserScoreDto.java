package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 점수 관련 DTO 클래스
 * 30년차 베테랑 개발자 품질의 종합 점수 시스템
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
public class UserScoreDto {

    /**
     * 사용자 점수 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 점수 정보")
    public static class Response {
        
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        
        @Schema(description = "사용자명", example = "coinmaster")
        private String username;
        
        @Schema(description = "사용자 닉네임", example = "코인마스터")
        private String userNickname;
        
        @Schema(description = "커뮤니티 점수", example = "1250")
        private Integer communityScore;
        
        @Schema(description = "총 점수", example = "2450")
        private Integer totalScore;
        
        @Schema(description = "사용자 레벨", example = "5")
        private Integer userLevel;
        
        @Schema(description = "레벨", example = "5")
        private Integer level;
        
        @Schema(description = "레벨명", example = "골드투자자")
        private String levelName;
        
        @Schema(description = "현재 레벨 진행률", example = "75.5")
        private Double levelProgress;
        
        @Schema(description = "진행률 퍼센트", example = "75.5")
        private Double progressPercentage;
        
        @Schema(description = "다음 레벨까지 필요 점수", example = "250")
        private Integer nextLevelScore;
        
        @Schema(description = "다음 레벨까지 점수", example = "250")
        private Integer pointsToNextLevel;
        
        @Schema(description = "총 랭킹", example = "23")
        private Long totalRanking;
        
        @Schema(description = "활동 점수", example = "800")
        private Integer activityScore;
        
        @Schema(description = "정확도 점수", example = "87.5")
        private Double accuracyScore;
        
        @Schema(description = "기여도 점수", example = "450")
        private Integer contributionScore;
        
        @Schema(description = "뱃지 수", example = "12")
        private Integer badgeCount;
        
        @Schema(description = "달성 수", example = "8")
        private Integer achievementCount;
        
        @Schema(description = "전문가 인증 여부", example = "true")
        private Boolean isExpert;
        
        @Schema(description = "마지막 업데이트 시간")
        private LocalDateTime lastUpdated;
    }

    /**
     * 점수 업데이트 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 점수 업데이트 요청")
    public static class UpdateRequest {
        
        @Schema(description = "점수 변경 유형", example = "POST_CREATED")
        @NotBlank(message = "점수 변경 유형은 필수입니다")
        private String scoreType;
        
        @Schema(description = "활동 유형", example = "ANALYSIS")
        private String activityType;
        
        @Schema(description = "점수 변경값", example = "50")
        @Min(value = -1000, message = "점수 변경값은 -1000 이상이어야 합니다")
        @Max(value = 1000, message = "점수 변경값은 1000 이하여야 합니다")
        private Integer scoreValue;
        
        @Schema(description = "추가 점수", example = "50")
        @Min(value = -1000, message = "추가 점수는 -1000 이상이어야 합니다")
        @Max(value = 1000, message = "추가 점수는 1000 이하여야 합니다")
        private Integer additionalScore;
        
        @Schema(description = "변경 사유", example = "유용한 게시글 작성")
        private String reason;
        
        @Schema(description = "관련 리소스 ID", example = "123")
        private Long resourceId;
    }

    /**
     * 랭킹 정보 DTO
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 랭킹 정보")
    public static class Ranking {
        
        @Schema(description = "랭킹 순위", example = "15")
        private Long rank;
        
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        
        @Schema(description = "사용자명", example = "coinmaster")
        private String username;
        
        @Schema(description = "사용자 닉네임", example = "코인마스터")
        private String nickname;
        
        @Schema(description = "레벨명", example = "골드투자자")
        private String levelName;
        
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
        
        @Schema(description = "총 점수", example = "1250")
        private Integer totalScore;
        
        @Schema(description = "레벨", example = "5")
        private Integer level;
        
        @Schema(description = "뱃지 수", example = "12")
        private Integer badgeCount;
        
        @Schema(description = "정확도 점수", example = "87.5")
        private Double accuracyScore;
        
        @Schema(description = "전문가 인증 여부", example = "true")
        private Boolean isExpert;
        
        @Schema(description = "변동 점수 (일주일 기준)", example = "+125")
        private Integer scoreChange;
    }

    /**
     * 랭킹 필터 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "랭킹 필터 조건")
    public static class RankingFilter {
        
        @Schema(description = "랭킹 유형", example = "TOTAL", allowableValues = {"TOTAL", "WEEKLY", "MONTHLY", "EXPERT"})
        @Builder.Default
        private String rankingType = "TOTAL";
        
        @Schema(description = "기간 (일)", example = "7")
        @Min(value = 1, message = "기간은 1일 이상이어야 합니다")
        @Max(value = 365, message = "기간은 365일 이하여야 합니다")
        @Builder.Default
        private Integer period = 7;
        
        @Schema(description = "최소 레벨", example = "1")
        @Min(value = 1, message = "최소 레벨은 1 이상이어야 합니다")
        private Integer minLevel;
        
        @Schema(description = "전문가만 조회 여부", example = "false")
        @Builder.Default
        private Boolean expertOnly = false;
    }

    /**
     * 상위 사용자 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상위 사용자 정보")
    public static class TopUser {
        
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        
        @Schema(description = "사용자명", example = "coinmaster")
        private String username;
        
        @Schema(description = "닉네임", example = "코인마스터")
        private String nickname;
        
        @Schema(description = "레벨명", example = "골드투자자")
        private String levelName;
        
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
        
        @Schema(description = "전문가 인증 여부", example = "true")
        private Boolean isExpert;
        
        @Schema(description = "총 점수", example = "1250")
        private Integer totalScore;
        
        @Schema(description = "레벨", example = "5")
        private Integer level;
        
        @Schema(description = "정확도 점수", example = "87.5")
        private Double accuracyScore;
        
        @Schema(description = "최근 활동 시간")
        private LocalDateTime lastActive;
    }

    /**
     * 레벨 시스템 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "레벨 시스템 정보")
    public static class LevelInfo {
        
        @Schema(description = "레벨", example = "5")
        private Integer level;
        
        @Schema(description = "레벨명", example = "숙련자")
        private String name;
        
        @Schema(description = "레벨명", example = "숙련자")
        private String levelName;
        
        @Schema(description = "필요 점수", example = "1000")
        private Integer requiredScore;
        
        @Schema(description = "최소 점수", example = "500")
        private Integer minScore;
        
        @Schema(description = "최대 점수", example = "999")
        private Integer maxScore;
        
        @Schema(description = "다음 레벨까지 점수", example = "500")
        private Integer nextLevelScore;
        
        @Schema(description = "해당 레벨 사용자 수", example = "50")
        private Integer userCount;
        
        @Schema(description = "레벨 설명")
        private String description;
        
        @Schema(description = "레벨 색상", example = "#4CAF50")
        private String color;
        
        @Schema(description = "레벨 아이콘", example = "star")
        private String icon;
        
        @Schema(description = "특별 권한 목록")
        private List<String> privileges;
    }

    /**
     * 달성 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 달성 정보")
    public static class Achievement {
        
        @Schema(description = "달성 ID", example = "first_post")
        private String id;
        
        @Schema(description = "달성 ID (숫자)", example = "1")
        private Long achievementId;
        
        @Schema(description = "달성명", example = "첫 게시글")
        private String title;
        
        @Schema(description = "달성 설명", example = "첫 번째 게시글을 작성했습니다")
        private String description;
        
        @Schema(description = "달성 여부", example = "true")
        private Boolean unlocked;
        
        @Schema(description = "달성 시간")
        private LocalDateTime unlockedAt;
        
        @Schema(description = "언락 날짜")
        private LocalDateTime unlockedDate;
        
        @Schema(description = "보상 점수", example = "100")
        private Integer rewardScore;
        
        @Schema(description = "달성 아이콘", example = "edit")
        private String icon;
        
        @Schema(description = "아이콘 URL")
        private String iconUrl;
        
        @Schema(description = "달성 카테고리", example = "COMMUNITY")
        private String category;
        
        @Schema(description = "희귀도", example = "COMMON", allowableValues = {"COMMON", "RARE", "EPIC", "LEGENDARY"})
        private String rarity;
    }

    /**
     * 점수 히스토리 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "점수 변경 히스토리")
    public static class ScoreHistory {
        
        @Schema(description = "히스토리 ID", example = "1")
        private Long id;
        
        @Schema(description = "점수 변경 유형", example = "POST_CREATED")
        private String scoreType;
        
        @Schema(description = "활동 유형", example = "CURRENT")
        private String activityType;
        
        @Schema(description = "점수 변경값", example = "50")
        private Integer scoreChange;
        
        @Schema(description = "변경 전 점수", example = "1200")
        private Integer previousScore;
        
        @Schema(description = "변경 후 점수", example = "1250")
        private Integer newScore;
        
        @Schema(description = "총 점수", example = "1250")
        private Integer totalScore;
        
        @Schema(description = "변경 사유", example = "유용한 게시글 작성")
        private String reason;
        
        @Schema(description = "관련 리소스 ID", example = "123")
        private Long resourceId;
        
        @Schema(description = "변경 시간")
        private LocalDateTime createdAt;
        
        @Schema(description = "변경 날짜")
        private LocalDateTime date;
    }

    /**
     * 활동 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 활동 통계")
    public static class ActivityStats {
        
        @Schema(description = "총 게시글 수", example = "45")
        private Long totalPosts;
        
        @Schema(description = "총 분석글 수", example = "15")
        private Integer totalAnalysis;
        
        @Schema(description = "총 댓글 수", example = "128")
        private Long totalComments;
        
        @Schema(description = "총 분석글 수", example = "12")
        private Long totalAnalyses;
        
        @Schema(description = "받은 좋아요 수", example = "234")
        private Long totalLikes;
        
        @Schema(description = "받은 북마크 수", example = "67")
        private Long totalBookmarks;
        
        @Schema(description = "받은 좋아요 총 수", example = "234")
        private Long totalLikesReceived;
        
        @Schema(description = "평균 정확도", example = "87.5")
        private Double averageAccuracy;
        
        @Schema(description = "최근 7일 활동 점수", example = "150")
        private Integer weeklyActivity;
        
        @Schema(description = "최근 30일 활동 점수", example = "620")
        private Integer monthlyActivity;
        
        @Schema(description = "연속 활동 일수", example = "15")
        private Integer streakDays;
        
        @Schema(description = "가장 긴 연속 활동 일수", example = "32")
        private Integer longestStreak;
        
        @Schema(description = "첫 활동 날짜")
        private LocalDateTime firstActivity;
    }

    /**
     * 뱃지 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 뱃지 정보")
    public static class Badge {
        
        @Schema(description = "뱃지 ID", example = "expert_trader")
        private String id;
        
        @Schema(description = "뱃지 ID (숫자)", example = "1")
        private Long badgeId;
        
        @Schema(description = "뱃지명", example = "전문 트레이더")
        private String name;
        
        @Schema(description = "뱃지명", example = "전문 트레이더")
        private String badgeName;
        
        @Schema(description = "뱃지 설명", example = "정확한 시장 분석으로 인정받은 전문가")
        private String description;
        
        @Schema(description = "뱃지 아이콘", example = "trending_up")
        private String icon;
        
        @Schema(description = "아이콘 URL")
        private String iconUrl;
        
        @Schema(description = "뱃지 색상", example = "#FFD700")
        private String color;
        
        @Schema(description = "획득 여부", example = "true")
        private Boolean earned;
        
        @Schema(description = "획득 시간")
        private LocalDateTime earnedAt;
        
        @Schema(description = "획득 날짜")
        private LocalDateTime earnedDate;
        
        @Schema(description = "뱃지 등급", example = "GOLD", allowableValues = {"BRONZE", "SILVER", "GOLD", "PLATINUM"})
        private String grade;
        
        @Schema(description = "뱃지 카테고리", example = "TRADING")
        private String category;
        
        @Schema(description = "희귀도", example = "RARE")
        private String rarity;
    }

    /**
     * 리더보드 엔트리 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리더보드 엔트리")
    public static class LeaderboardEntry {
        
        @Schema(description = "순위", example = "1")
        private Long position;
        
        @Schema(description = "랭킹 순위", example = "1")
        private Integer rank;
        
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        
        @Schema(description = "사용자명", example = "coinmaster")
        private String username;
        
        @Schema(description = "닉네임", example = "코인마스터")
        private String nickname;
        
        @Schema(description = "점수", example = "2450")
        private Integer score;
        
        @Schema(description = "총 점수", example = "2450")
        private Integer totalScore;
        
        @Schema(description = "점수 변동", example = "+125")
        private Integer scoreChange;
        
        @Schema(description = "레벨", example = "7")
        private Integer level;
        
        @Schema(description = "레벨명", example = "골드투자자")
        private String levelName;
        
        @Schema(description = "뱃지 수", example = "15")
        private Integer badgeCount;
        
        @Schema(description = "전문가 인증 여부", example = "true")
        private Boolean isExpert;
        
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
    }

    /**
     * 전문가 사용자 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "전문가 사용자 정보")
    public static class ExpertUser {
        
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        
        @Schema(description = "사용자명", example = "coinmaster")
        private String username;
        
        @Schema(description = "닉네임", example = "코인마스터")
        private String nickname;
        
        @Schema(description = "전문 분야", example = "비트코인 분석")
        private String specialization;
        
        @Schema(description = "총 점수", example = "2450")
        private Integer totalScore;
        
        @Schema(description = "정확도 점수", example = "92.5")
        private Double accuracyScore;
        
        @Schema(description = "총 분석글 수", example = "87")
        private Long totalAnalyses;
        
        @Schema(description = "분석 수", example = "87")
        private Integer analysisCount;
        
        @Schema(description = "평균 정확도", example = "92.5")
        private Double averageAccuracy;
        
        @Schema(description = "레벨", example = "7")
        private Integer level;
        
        @Schema(description = "전문가 인증 시기")
        private LocalDateTime expertSince;
        
        @Schema(description = "성공한 예측 수", example = "80")
        private Long successfulPredictions;
        
        @Schema(description = "전문가 인증 날짜")
        private LocalDateTime certifiedAt;
        
        @Schema(description = "최근 활동 날짜")
        private LocalDateTime lastActive;
        
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
        
        @Schema(description = "전문가 등급", example = "GOLD", allowableValues = {"BRONZE", "SILVER", "GOLD", "PLATINUM"})
        private String expertGrade;
    }
}
