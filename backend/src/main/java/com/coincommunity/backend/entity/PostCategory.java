package com.coincommunity.backend.entity;

/**
 * 게시글 카테고리 정의 열거형
 */
public enum PostCategory {
    FREE("자유게시판"),
    DISCUSSION("토론"),
    QUESTION("질문"),
    INFO("정보공유"),
    NOTICE("공지사항");

    private final String displayName;

    PostCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
