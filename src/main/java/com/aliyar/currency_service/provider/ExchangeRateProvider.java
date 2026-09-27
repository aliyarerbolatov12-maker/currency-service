package com.aliyar.currency_service.provider;

import com.aliyar.currency_service.provider.dto.ExchangeRateDto;

import java.util.List;

public interface ExchangeRateProvider {
    List<ExchangeRateDto> fetchRates(String base);

    ExchangeRateDto fetchSpecificRate(String base, String quote);

    List<String> getSupportedCurrencies();

    String getProviderName();
}