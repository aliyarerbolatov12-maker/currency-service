package com.aliyar.currency_service.provider;

import com.aliyar.currency_service.client.FrankfurterClient;
import com.aliyar.currency_service.client.dto.CurrencyResponse;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import com.aliyar.currency_service.provider.exception.ProviderUnavailableException;
import com.aliyar.currency_service.provider.mapper.FrankfurterMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrankfurterProvider implements ExchangeRateProvider {

    private final FrankfurterClient client;
    private final FrankfurterMapper mapper;

    @Override
    @Retry(name = "frankfurterRetry")
    @CircuitBreaker(name = "frankfurterCB", fallbackMethod = "fetchRatesFallback")
    public List<ExchangeRateDto> fetchRates(String base) {
        return client.getLatestRates(base).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Retry(name = "frankfurterRetry")
    @CircuitBreaker(name = "frankfurterCB", fallbackMethod = "fetchSpecificRateFallback")
    public ExchangeRateDto fetchSpecificRate(String base, String quote) {
        return mapper.toDto(client.getSpecificRate(base, quote));
    }

    @Override
    @Retry(name = "frankfurterRetry")
    @CircuitBreaker(name = "frankfurterCB", fallbackMethod = "getSupportedCurrenciesFallback")
    public List<String> getSupportedCurrencies() {
        return client.getSupportedCurrencies().stream()
                .map(CurrencyResponse::isoCode)
                .toList();
    }

    @Override
    public String getProviderName() {
        return "FRANKFURTER";
    }

    public List<ExchangeRateDto> fetchRatesFallback(String base, Throwable ex) {
        log.error("Failed to fetch exchange rates from provider '{}' for base '{}'",
                getProviderName(), base, ex);

        throw new ProviderUnavailableException(getProviderName(), ex);
    }

    public ExchangeRateDto fetchSpecificRateFallback(String base, String quote, Throwable ex) {
        log.error("Failed to fetch exchange rate from provider '{}' for {}/{}",
                getProviderName(), base, quote, ex);

        throw new ProviderUnavailableException(getProviderName(), ex);
    }

    public List<String> getSupportedCurrenciesFallback(Throwable ex) {
        log.error("Failed to fetch supported currencies from provider '{}'",
                getProviderName(), ex);

        throw new ProviderUnavailableException(getProviderName(), ex);
    }
}