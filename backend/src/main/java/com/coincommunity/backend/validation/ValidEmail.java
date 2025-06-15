package com.coincommunity.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 강화된 이메일 검증 어노테이션
 * 
 * 기본 @Email보다 엄격한 검증:
 * - 도메인 블랙리스트 검사
 * - 임시 이메일 서비스 차단
 * - 길이 제한
 */
@Documented
@Constraint(validatedBy = EnhancedEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    String message() default "유효하지 않은 이메일 주소입니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    boolean allowTemporaryEmail() default false;
    int maxLength() default 320; // RFC 5321 표준
}
