package com.coincommunity.backend.entity;

import com.coincommunity.backend.dto.PriceAlertDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가격 알림 엔티티
 */
@Entity
@Table(name = "price_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceAlertDto.AlertType alertType;

    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal targetPrice;

    @Column(precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceAlertDto.AlertStatus status;

    @Column
    private boolean repeat;

    @Column
    private LocalDateTime lastTriggeredAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 알림 상태 업데이트
     */
    public void updateStatus(PriceAlertDto.AlertStatus status) {
        this.status = status;
    }

    /**
     * 알림 트리거 시간 업데이트
     */
    public void updateLastTriggeredAt(LocalDateTime time) {
        this.lastTriggeredAt = time;
    }

    /**
     * 현재 가격 업데이트
     */
    public void updateCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * 알림 조건 충족 여부 확인
     * 현재 가격과 목표 가격을 비교하여 알림 조건(이상/이하/변동률)이 충족되었는지 확인
     */
    public boolean checkAlertCondition() {
        if (currentPrice == null || targetPrice == null) {
            return false;
        }

        switch (alertType) {
            case ABOVE:
                // 현재 가격이 목표 가격 이상인 경우
                return currentPrice.compareTo(targetPrice) >= 0;

            case BELOW:
                // 현재 가격이 목표 가격 이하인 경우
                return currentPrice.compareTo(targetPrice) <= 0;

            case PERCENT_CHANGE_UP:
                // 가격 변동률이 목표 퍼센트 이상인 경우
                // 이전 가격이 필요하므로 실제 구현에서는 추가 데이터가 필요함
                // 여기서는 간단히 현재 가격이 목표 가격의 X% 이상인지만 확인
                if (lastTriggeredAt == null) {
                    return true; // 첫 트리거인 경우 일단 알림
                }
                return false; // 실제 구현에서는 가격 변동률 계산 필요

            default:
                return false;
        }
    }
}
