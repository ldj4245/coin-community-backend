package com.coincommunity.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 사용자가 요청한 작업을 수행할 권한이 없을 때 발생하는 예외
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
    
    public UnauthorizedException() {
        super("이 작업을 수행할 권한이 없습니다.", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
}
