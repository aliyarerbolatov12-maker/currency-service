package com.aliyar.currency_service.modules.exception;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String providerName) {
        super("Provider not found: " + providerName);
    }
}