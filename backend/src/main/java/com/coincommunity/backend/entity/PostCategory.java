package com.coincommunity.backend.entity;

/**
 * 게시글 카테고리 정의 열거형
 */
public enum PostCategory {
    FREE("자유게시판"),
    MARKET_ANALYSIS("시장분석"),
    COIN_INFO("코인정보"),
    EXCHANGE_INFO("거래소정보"),
    BEGINNER("초보자가이드"),
    MINING("채굴"),
    NFT("NFT"),
    NOTICE("공지사항");

    private final String displayName;

    PostCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
