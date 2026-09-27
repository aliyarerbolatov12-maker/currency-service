package com.aliyar.currency_service.provider;

import com.aliyar.currency_service.client.FrankfurterClient;
import com.aliyar.currency_service.client.dto.CurrencyResponse;
import com.aliyar.currency_service.client.dto.ExchangeRateResponse;
import com.aliyar.currency_service.client.dto.RateResponse;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import com.aliyar.currency_service.provider.exception.ProviderUnavailableException;
import com.aliyar.currency_service.provider.mapper.FrankfurterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrankfurterProviderTest {

    @Mock
    FrankfurterClient client;

    @Mock
    FrankfurterMapper mapper;

    FrankfurterProvider provider;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        provider = new FrankfurterProvider(client, mapper);
    }

    @Test
    void fetchRates_success_returnsMappedDtos() {

        RateResponse rateResponse = new RateResponse(
                LocalDate.of(2026, 9, 25), "USD", "EUR", new BigDecimal("0.92"));

        ExchangeRateDto dto = new ExchangeRateDto(
                "USD", "EUR", new BigDecimal("0.92"), Instant.parse("2026-09-25T00:00:00Z"), "FRANKFURTER");

        when(client.getLatestRates("USD")).thenReturn(List.of(rateResponse));
        when(mapper.toDto(rateResponse)).thenReturn(dto);

        List<ExchangeRateDto> result = provider.fetchRates("USD");

        assertThat(result).containsExactly(dto);
        verify(client).getLatestRates("USD");
        verify(mapper).toDto(rateResponse);
    }

    @Test
    void fetchSpecificRate_success_returnsMappedDto() {

        ExchangeRateResponse response = new ExchangeRateResponse(
                "USD", "EUR", new BigDecimal("0.92"), LocalDate.of(2026, 9, 25));

        ExchangeRateDto dto = new ExchangeRateDto(
                "USD", "EUR", new BigDecimal("0.92"), Instant.parse("2026-09-25T00:00:00Z"), "FRANKFURTER");

        when(client.getSpecificRate("USD", "EUR")).thenReturn(response);
        when(mapper.toDto(response)).thenReturn(dto);

        ExchangeRateDto result = provider.fetchSpecificRate("USD", "EUR");

        assertThat(result).isEqualTo(dto);
        verify(client).getSpecificRate("USD", "EUR");
        verify(mapper).toDto(response);
    }

    @Test
    void getSupportedCurrencies_success_returnsIsoCodes() {

        CurrencyResponse usd = new CurrencyResponse("USD", "840", "US Dollar", "$");
        CurrencyResponse eur = new CurrencyResponse("EUR", "978", "Euro", "€");

        when(client.getSupportedCurrencies()).thenReturn(List.of(usd, eur));

        List<String> result = provider.getSupportedCurrencies();

        assertThat(result).containsExactly("USD", "EUR");
        verify(client).getSupportedCurrencies();
    }

    @Test
    void getProviderName_returnsFrankfurter() {
        assertThat(provider.getProviderName()).isEqualTo("FRANKFURTER");
    }

    @Test
    void fetchRatesFallback_throwsProviderUnavailableException() {

        RuntimeException cause = new RuntimeException("timeout");

        assertThatThrownBy(() -> provider.fetchRatesFallback("USD", cause))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasCauseReference(cause)
                .hasMessageContaining("FRANKFURTER");
    }

    @Test
    void fetchSpecificRateFallback_throwsProviderUnavailableException() {

        RuntimeException cause = new RuntimeException("circuit open");

        assertThatThrownBy(() -> provider.fetchSpecificRateFallback("USD", "EUR", cause))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasCauseReference(cause)
                .hasMessageContaining("FRANKFURTER");
    }

    @Test
    void getSupportedCurrenciesFallback_throwsProviderUnavailableException() {

        RuntimeException cause = new RuntimeException("connection refused");

        assertThatThrownBy(() -> provider.getSupportedCurrenciesFallback(cause))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasCauseReference(cause)
                .hasMessageContaining("FRANKFURTER");
    }
}