package com.coincommunity.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외 클래스
 * 비즈니스 로직에서 발생하는 예외 상황을 처리하기 위한 클래스
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * 생성자
     * 
     * @param message 예외 메시지
     * @param errorCode 에러 코드
     * @param httpStatus HTTP 상태 코드
     */
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 생성자
     * 
     * @param message 예외 메시지
     * @param errorCode 에러 코드
     * @param httpStatus HTTP 상태 코드
     * @param cause 원인 예외
     */
    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 기본 생성자 (BAD_REQUEST)
     * 
     * @param message 예외 메시지
     */
    public BusinessException(String message) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }

    /**
     * 원인 예외와 함께 생성하는 생성자
     * 
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public BusinessException(String message, Throwable cause) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
