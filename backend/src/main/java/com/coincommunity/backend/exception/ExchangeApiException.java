package com.coincommunity.backend.exception;

/**
 * 거래소 API 관련 예외 클래스
 */
public class ExchangeApiException extends RuntimeException {

    private final String exchangeName;

    public ExchangeApiException(String exchangeName, String message) {
        super(message);
        this.exchangeName = exchangeName;
    }

    public ExchangeApiException(String exchangeName, String message, Throwable cause) {
        super(message, cause);
        this.exchangeName = exchangeName;
    }

    public String getExchangeName() {
        return exchangeName;
    }
}
