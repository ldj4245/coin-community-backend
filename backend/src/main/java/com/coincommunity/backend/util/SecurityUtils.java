package com.coincommunity.backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 보안 관련 유틸리티 클래스
 */
public class SecurityUtils {
    
    /**
     * 현재 인증된 사용자의 ID를 반환
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            try {
                return Long.parseLong(((UserDetails) principal).getUsername());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * UserDetails에서 사용자 ID를 추출
     */
    public static Long extractUserId(UserDetails userDetails) {
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userDetails.getUsername());
        }
    }
    
    /**
     * 인증 여부 확인
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
               && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
