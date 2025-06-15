package com.coincommunity.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 강화된 비밀번호 검증 어노테이션
 * 
 * 검증 기준:
 * - 최소/최대 길이
 * - 대소문자, 숫자, 특수문자 포함
 * - 일반적인 패스워드 패턴 차단
 * - 사용자 정보와 유사성 검사
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "비밀번호가 보안 요구사항을 만족하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    int minLength() default 8;
    int maxLength() default 128;
    boolean requireUppercase() default true;
    boolean requireLowercase() default true;
    boolean requireDigits() default true;
    boolean requireSpecialChars() default true;
    boolean checkCommonPasswords() default true;
}
