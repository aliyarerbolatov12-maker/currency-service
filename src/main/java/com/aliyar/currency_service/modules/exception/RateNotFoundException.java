package com.aliyar.currency_service.modules.exception;

public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String base, String quote) {
        super("Rate not found for " + base + "/" + quote);
    }

    public RateNotFoundException() {
        super("No exchange rates available");
    }
}