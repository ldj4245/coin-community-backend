package com.coincommunity.backend.exception;

import com.coincommunity.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 전역 예외 처리기
 * 애플리케이션 전체에서 발생하는 예외를 처리합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 일반적인 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("예상치 못한 오류가 발생했습니다: {}", ex.getMessage(), ex);
        
        ApiResponse<Object> response = ApiResponse.error("서버 오류가 발생했습니다.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("잘못된 요청 파라미터: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 비즈니스 예외 처리 (ResourceNotFoundException, UnauthorizedException 등 포함)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("비즈니스 예외: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    /**
     * 외부 거래소 API 호출 예외 처리
     */
    @ExceptionHandler(ExchangeApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleExchangeApiException(ExchangeApiException ex, WebRequest request) {
        log.error("거래소 API 오류 ({}): {}", ex.getExchangeName(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("EXCHANGE_API_ERROR", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    /**
     * 요청 파라미터 검증 실패 처리
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));
        ApiResponse<Object> response = ApiResponse.error("VALIDATION_ERROR", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("런타임 오류가 발생했습니다: {}", ex.getMessage(), ex);
        
        ApiResponse<Object> response = ApiResponse.error("RUNTIME_ERROR", "처리 중 오류가 발생했습니다.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * JSON 파싱 예외 처리 (잘못된 요청 본문)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        String message = ex.getMessage();
        String errorCode = "INVALID_REQUEST";
        
        // 열거형 변환 오류 처리
        if (message.contains("Enum")) {
            if (message.contains("PostCategory")) {
                message = "유효하지 않은 게시글 카테고리입니다. /posts/categories 엔드포인트에서 유효한 카테고리 목록을 확인하세요.";
                errorCode = "INVALID_CATEGORY";
            } else {
                message = "유효하지 않은 열거형 값입니다.";
                errorCode = "INVALID_ENUM_VALUE";
            }
        }
        
        log.warn("잘못된 요청 본문: {}", message);
        ApiResponse<Object> response = ApiResponse.error(errorCode, message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}