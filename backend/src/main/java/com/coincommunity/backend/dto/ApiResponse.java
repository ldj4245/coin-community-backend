package com.coincommunity.backend.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답의 표준 형식을 정의하는 클래스
 * @param <T> 응답 데이터의 타입
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    /**
     * 성공 응답을 생성합니다.
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공적으로 처리되었습니다.", data);
    }

    /**
     * 성공 응답을 생성합니다 (메시지 포함).
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> successWithMessage(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 메시지만 있는 성공 응답을 생성합니다.
     * @param message 성공 메시지
     * @return 성공 응답 객체
     */
    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    /**
     * 오류 응답을 생성합니다.
     * @param message 오류 메시지
     * @return 오류 응답 객체
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    /**
     * 오류 응답을 생성합니다.
     * @param message 오류 메시지
     * @param data 오류 데이터
     * @return 오류 응답 객체
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
