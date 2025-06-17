package com.coincommunity.backend.entity;

/**
 * 게시글 카테고리 정의 열거형
 */
public enum PostCategory {
    FREE("자유게시판"),
    MARKET_ANALYSIS("시장 분석"),
    COIN_INFO("코인정보"),
    EXCHANGE_INFO("거래소정보"),
    BEGINNER("초보자가이드"),
    MINING("채굴"),
    NFT("NFT"),
    NOTICE("공지사항"),
    TRADING_STRATEGY("트레이딩 전략"),
    DEFI("디파이"),
    ALTCOIN_DISCUSSION("알트코인 토론"),
    BITCOIN("비트코인"),
    ETHEREUM("이더리움"),
    TECHNICAL_ANALYSIS("기술적 분석"),
    FUNDAMENTAL_ANALYSIS("기본적 분석"),
    PRICE_PREDICTION("가격 예측"),
    COMMUNITY_EVENT("커뮤니티 이벤트"),
    QUESTION("질문게시판"),
    INFORMATION("정보게시판"),
    NEWS("뉴스");

    private final String displayName;

    PostCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
