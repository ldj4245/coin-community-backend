package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 암호화폐 분석 좋아요 엔티티
 * 사용자가 분석글에 대한 좋아요를 표현하는 기능
 */
@Entity
@Table(
    name = "analysis_likes",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "analysis_id"},
        name = "uk_analysis_like_user_analysis"
    ),
    indexes = {
        @Index(name = "idx_analysis_like_user", columnList = "user_id"),
        @Index(name = "idx_analysis_like_analysis", columnList = "analysis_id"),
        @Index(name = "idx_analysis_like_created", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 좋아요를 누른 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_analysis_like_user"))
    private User user;

    /**
     * 좋아요를 받은 분석글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, foreignKey = @ForeignKey(name = "fk_analysis_like_analysis"))
    private CoinAnalysis analysis;

    /**
     * 좋아요 생성일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 좋아요 타입 (기본값: LIKE)
     * LIKE: 일반 좋아요, DISLIKE: 싫어요 (향후 확장 가능)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "like_type", nullable = false, length = 20)
    @Builder.Default
    private LikeType likeType = LikeType.LIKE;

    /**
     * 좋아요 타입 enum
     */
    public enum LikeType {
        LIKE("좋아요"),
        DISLIKE("싫어요");

        private final String description;

        LikeType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 비즈니스 로직: 사용자와 분석글 연관관계 설정
     */
    public void setUserAndAnalysis(User user, CoinAnalysis analysis) {
        this.user = user;
        this.analysis = analysis;
    }

    /**
     * 비즈니스 로직: 좋아요 타입 변경
     */
    public void toggleLikeType() {
        this.likeType = (this.likeType == LikeType.LIKE) ? LikeType.DISLIKE : LikeType.LIKE;
    }

    /**
     * 비즈니스 로직: 좋아요인지 확인
     */
    public boolean isLike() {
        return this.likeType == LikeType.LIKE;
    }

    /**
     * 비즈니스 로직: 싫어요인지 확인
     */
    public boolean isDislike() {
        return this.likeType == LikeType.DISLIKE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisLike)) return false;
        AnalysisLike that = (AnalysisLike) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AnalysisLike{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", analysisId=" + (analysis != null ? analysis.getId() : null) +
                ", likeType=" + likeType +
                ", createdAt=" + createdAt +
                '}';
    }
}