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
 * 암호화폐 분석 북마크 엔티티
 * 사용자가 관심있는 분석글을 북마크로 저장하는 기능
 */
@Entity
@Table(
    name = "analysis_bookmarks",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "analysis_id"},
        name = "uk_analysis_bookmark_user_analysis"
    ),
    indexes = {
        @Index(name = "idx_analysis_bookmark_user", columnList = "user_id"),
        @Index(name = "idx_analysis_bookmark_analysis", columnList = "analysis_id"),
        @Index(name = "idx_analysis_bookmark_created", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 북마크한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_analysis_bookmark_user"))
    private User user;

    /**
     * 북마크된 분석글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, foreignKey = @ForeignKey(name = "fk_analysis_bookmark_analysis"))
    private CoinAnalysis analysis;

    /**
     * 북마크 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 북마크 메모 (선택사항)
     */
    @Column(name = "memo", length = 500)
    private String memo;

    /**
     * 북마크 태그 (선택사항)
     */
    @Column(name = "tags", length = 200)
    private String tags;

    /**
     * 북마크 활성화 상태
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 편의 메서드들

    /**
     * 사용자와 분석글로 북마크 생성
     */
    public static AnalysisBookmark of(User user, CoinAnalysis analysis) {
        return AnalysisBookmark.builder()
                .user(user)
                .analysis(analysis)
                .build();
    }

    /**
     * 사용자와 분석글, 메모로 북마크 생성
     */
    public static AnalysisBookmark of(User user, CoinAnalysis analysis, String memo) {
        return AnalysisBookmark.builder()
                .user(user)
                .analysis(analysis)
                .memo(memo)
                .build();
    }

    /**
     * 북마크 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 북마크 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 메모 업데이트
     */
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    /**
     * 태그 업데이트
     */
    public void updateTags(String tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisBookmark)) return false;
        AnalysisBookmark that = (AnalysisBookmark) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AnalysisBookmark{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", analysisId=" + (analysis != null ? analysis.getId() : null) +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                '}';
    }
}