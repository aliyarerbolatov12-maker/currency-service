package com.aliyar.currency_service.modules.exception;

public class InvalidCurrencyPairException extends RuntimeException {

    public InvalidCurrencyPairException(String base, String quote) {
        super("Currency pair is invalid: base and quote must differ, got '%s' and '%s'"
                .formatted(base, quote));
    }
}