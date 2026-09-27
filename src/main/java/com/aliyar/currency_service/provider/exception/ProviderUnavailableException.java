package com.aliyar.currency_service.provider.exception;

public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String providerName, Throwable cause) {
        super("Provider unavailable: " + providerName, cause);
    }
}