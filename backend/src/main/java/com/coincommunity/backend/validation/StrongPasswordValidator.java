package com.coincommunity.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 강화된 비밀번호 검증기
 * 
 * 보안 검증 기능:
 * - 길이 및 복잡성 검증
 * - 일반적인 비밀번호 패턴 차단
 * - 연속된 문자/숫자 패턴 검사
 * - 키보드 패턴 검사
 */
@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    // 일반적으로 사용되는 약한 비밀번호들
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
        "password", "password123", "123456", "123456789", "qwerty",
        "abc123", "111111", "password1", "1234567890", "123123",
        "admin", "root", "user", "guest", "test", "demo",
        "welcome", "login", "pass", "secret", "access"
    ));
    
    // 연속된 문자 패턴
    private static final Pattern SEQUENTIAL_CHARS = Pattern.compile("(.)\\1{2,}");
    private static final Pattern SEQUENTIAL_NUMBERS = Pattern.compile("(012|123|234|345|456|567|678|789|890)");
    private static final Pattern KEYBOARD_PATTERNS = Pattern.compile("(qwe|wer|ert|rty|tyu|yui|uio|iop|asd|sdf|dfg|fgh|ghj|hjk|jkl|zxc|xcv|cvb|vbn|bnm)");
    
    private int minLength;
    private int maxLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChars;
    private boolean checkCommonPasswords;
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigits = constraintAnnotation.requireDigits();
        this.requireSpecialChars = constraintAnnotation.requireSpecialChars();
        this.checkCommonPasswords = constraintAnnotation.checkCommonPasswords();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // 길이 검증
        if (password.length() < minLength) {
            addConstraintViolation(context, "비밀번호는 최소 " + minLength + "자 이상이어야 합니다");
            return false;
        }
        
        if (password.length() > maxLength) {
            addConstraintViolation(context, "비밀번호는 최대 " + maxLength + "자 이하여야 합니다");
            return false;
        }
        
        // 대문자 검증
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            addConstraintViolation(context, "비밀번호에 대문자가 포함되어야 합니다");
            return false;
        }
        
        // 소문자 검증
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            addConstraintViolation(context, "비밀번호에 소문자가 포함되어야 합니다");
            return false;
        }
        
        // 숫자 검증
        if (requireDigits && !password.matches(".*\\d.*")) {
            addConstraintViolation(context, "비밀번호에 숫자가 포함되어야 합니다");
            return false;
        }
        
        // 특수문자 검증
        if (requireSpecialChars && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            addConstraintViolation(context, "비밀번호에 특수문자가 포함되어야 합니다");
            return false;
        }
        
        // 일반적인 비밀번호 검사
        if (checkCommonPasswords && isCommonPassword(password)) {
            addConstraintViolation(context, "일반적으로 사용되는 비밀번호는 사용할 수 없습니다");
            return false;
        }
        
        // 연속된 문자 패턴 검사
        if (hasSequentialPatterns(password)) {
            addConstraintViolation(context, "연속된 문자나 숫자 패턴은 사용할 수 없습니다");
            return false;
        }
        
        // 키보드 패턴 검사
        if (hasKeyboardPatterns(password)) {
            addConstraintViolation(context, "키보드 패턴은 사용할 수 없습니다");
            return false;
        }
        
        return true;
    }
    
    /**
     * 일반적인 비밀번호인지 검사
     */
    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return COMMON_PASSWORDS.contains(lowerPassword) ||
               lowerPassword.startsWith("password") ||
               lowerPassword.startsWith("123456") ||
               lowerPassword.equals("qwerty123");
    }
    
    /**
     * 연속된 패턴 검사
     */
    private boolean hasSequentialPatterns(String password) {
        String lowerPassword = password.toLowerCase();
        
        // 같은 문자 3개 이상 연속
        if (SEQUENTIAL_CHARS.matcher(password).find()) {
            return true;
        }
        
        // 연속된 숫자
        if (SEQUENTIAL_NUMBERS.matcher(password).find()) {
            return true;
        }
        
        // 역순 연속 숫자
        String reversed = new StringBuilder(password).reverse().toString();
        if (SEQUENTIAL_NUMBERS.matcher(reversed).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 키보드 패턴 검사
     */
    private boolean hasKeyboardPatterns(String password) {
        String lowerPassword = password.toLowerCase();
        return KEYBOARD_PATTERNS.matcher(lowerPassword).find();
    }
    
    /**
     * 커스텀 에러 메시지 추가
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
