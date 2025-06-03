package com.coincommunity.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s를 찾을 수 없습니다. %s: '%s'", resourceName, fieldName, fieldValue), 
              "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
