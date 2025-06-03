package com.coincommunity.backend.entity;

/**
 * 사용자 권한 정의 열거형
 */
public enum UserRole {
    USER,       // 일반 사용자
    ADMIN,      // 관리자
    MODERATOR   // 중재자(게시판 관리자)
}
