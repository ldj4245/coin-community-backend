package com.coincommunity.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 강화된 이메일 검증기
 * 
 * 검증 기능:
 * - RFC 5322 표준 이메일 형식 검증
 * - 임시 이메일 서비스 도메인 차단
 * - 최대 길이 제한
 * - 특수 문자 및 보안 검증
 */
@Slf4j
public class EnhancedEmailValidator implements ConstraintValidator<ValidEmail, String> {

    // RFC 5322 표준 이메일 정규식 (간소화)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // 임시 이메일 서비스 도메인 블랙리스트
    private static final Set<String> TEMPORARY_EMAIL_DOMAINS = new HashSet<>(Arrays.asList(
        "10minutemail.com", "guerrillamail.com", "mailinator.com",
        "tempmail.org", "throwaway.email", "temp-mail.org",
        "yopmail.com", "dispostable.com", "maildrop.cc",
        "sharklasers.com", "grr.la", "guerrillamailblock.com"
    ));
    
    private boolean allowTemporaryEmail;
    private int maxLength;
    
    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.allowTemporaryEmail = constraintAnnotation.allowTemporaryEmail();
        this.maxLength = constraintAnnotation.maxLength();
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        email = email.trim().toLowerCase();
        
        // 길이 검증
        if (email.length() > maxLength) {
            addConstraintViolation(context, "이메일 주소가 너무 깁니다 (최대 " + maxLength + "자)");
            return false;
        }
        
        // 기본 형식 검증
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            addConstraintViolation(context, "이메일 형식이 올바르지 않습니다");
            return false;
        }
        
        // 도메인 추출
        String domain = email.substring(email.indexOf('@') + 1);
        
        // 임시 이메일 도메인 검증
        if (!allowTemporaryEmail && isTemporaryEmailDomain(domain)) {
            addConstraintViolation(context, "임시 이메일 주소는 사용할 수 없습니다");
            return false;
        }
        
        // 추가 보안 검증
        if (containsSuspiciousPatterns(email)) {
            addConstraintViolation(context, "허용되지 않는 문자가 포함되어 있습니다");
            return false;
        }
        
        return true;
    }
    
    /**
     * 임시 이메일 도메인 검사
     */
    private boolean isTemporaryEmailDomain(String domain) {
        return TEMPORARY_EMAIL_DOMAINS.contains(domain.toLowerCase());
    }
    
    /**
     * 의심스러운 패턴 검사
     */
    private boolean containsSuspiciousPatterns(String email) {
        // 연속된 점 검사
        if (email.contains("..")) {
            return true;
        }
        
        // 점으로 시작하거나 끝나는 경우
        if (email.startsWith(".") || email.endsWith(".")) {
            return true;
        }
        
        // @ 앞뒤로 점이 있는 경우
        if (email.contains(".@") || email.contains("@.")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 커스텀 에러 메시지 추가
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
