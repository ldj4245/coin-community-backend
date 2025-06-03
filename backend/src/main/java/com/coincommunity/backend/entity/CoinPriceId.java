package com.coincommunity.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * CoinPrice 엔티티의 복합키를 정의하는 클래스
 * coinId와 exchange의 조합으로 고유한 키를 생성합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinPriceId implements Serializable {

    private String coinId;
    private String exchange;
}
