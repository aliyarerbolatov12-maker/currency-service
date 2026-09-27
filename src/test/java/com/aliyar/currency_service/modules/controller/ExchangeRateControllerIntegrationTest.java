package com.aliyar.currency_service.modules.controller;

import com.aliyar.currency_service.common.exception.GlobalExceptionHandler;
import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.exception.HistoryPeriodTooLargeException;
import com.aliyar.currency_service.modules.exception.InvalidCurrencyPairException;
import com.aliyar.currency_service.modules.exception.InvalidDateRangeException;
import com.aliyar.currency_service.modules.exception.RateNotFoundException;
import com.aliyar.currency_service.modules.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
@Import(GlobalExceptionHandler.class)
class ExchangeRateControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExchangeRateService exchangeRateService;

    // ---------------------------------------------------------------
    // Success cases
    // ---------------------------------------------------------------

    @Test
    void updateRates_success_returns200() throws Exception {

        doNothing().when(exchangeRateService).updateAllRates();

        mockMvc.perform(post("/api/v1/rates/update"))
                .andExpect(status().isOk());

        verify(exchangeRateService).updateAllRates();
    }

    @Test
    void getHistory_success_returns200() throws Exception {

        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                1L, "USD", "EUR", new BigDecimal("0.92"), Instant.parse("2026-09-20T00:00:00Z"), "ECB");

        when(exchangeRateService.getExchangeRatesByCurrencyAndDateRange(any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/v1/rates/history")
                                .param("currency", "USD")
                                .param("start", "2026-09-10T00:00:00Z")
                                .param("end", "2026-09-20T00:00:00Z")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].base").value("USD"))
                .andExpect(jsonPath("$[0].quote").value("EUR"))
                .andExpect(jsonPath("$[0].rate").value(0.92))
                .andExpect(jsonPath("$[0].providerName").value("ECB"));

        verify(exchangeRateService).getExchangeRatesByCurrencyAndDateRange(any());
    }

    @Test
    void getCrossHistory_success_returns200() throws Exception {

        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                null, "GBP", "EUR", new BigDecimal("1.17"), Instant.parse("2026-09-20T00:00:00Z"), "computed");

        when(exchangeRateService.getCrossRateHistory(any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/v1/rates/history/cross")
                                .param("base", "GBP")
                                .param("quote", "EUR")
                                .param("start", "2026-09-10T00:00:00Z")
                                .param("end", "2026-09-20T00:00:00Z")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].base").value("GBP"))
                .andExpect(jsonPath("$[0].quote").value("EUR"))
                .andExpect(jsonPath("$[0].rate").value(1.17))
                .andExpect(jsonPath("$[0].providerName").value("computed"));

        verify(exchangeRateService).getCrossRateHistory(any());
    }

    @Test
    void getLatestPair_success_returns200() throws Exception {

        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                5L, "USD", "EUR", new BigDecimal("0.9123"), Instant.parse("2026-09-25T08:00:00Z"), "ECB");

        when(exchangeRateService.getLatestRate("USD", "EUR"))
                .thenReturn(dto);

        mockMvc.perform(
                        get("/api/v1/rates/latest/pair")
                                .param("base", "USD")
                                .param("quote", "EUR")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.quote").value("EUR"))
                .andExpect(jsonPath("$.rate").value(0.9123))
                .andExpect(jsonPath("$.providerName").value("ECB"));

        verify(exchangeRateService).getLatestRate("USD", "EUR");
    }

    @Test
    void getLatestByBase_success_returns200() throws Exception {

        ExchangeRateResponseDto dto1 = new ExchangeRateResponseDto(
                1L, "USD", "EUR", new BigDecimal("0.92"), Instant.parse("2026-09-25T08:00:00Z"), "ECB");
        ExchangeRateResponseDto dto2 = new ExchangeRateResponseDto(
                2L, "USD", "GBP", new BigDecimal("0.79"), Instant.parse("2026-09-25T08:00:00Z"), "ECB");

        when(exchangeRateService.getLatestRatesByBase(eq("USD"), eq(10)))
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/rates/latest/base/USD"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].quote").value("EUR"))
                .andExpect(jsonPath("$[1].quote").value("GBP"));

        verify(exchangeRateService).getLatestRatesByBase("USD", 10);
    }

    @Test
    void getLatestByBase_customLimit_success_returns200() throws Exception {

        when(exchangeRateService.getLatestRatesByBase(eq("USD"), eq(5)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/rates/latest/base/USD")
                                .param("limit", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(exchangeRateService).getLatestRatesByBase("USD", 5);
    }

    @Test
    void getMostRecent_success_returns200() throws Exception {

        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                7L, "USD", "JPY", new BigDecimal("147.35"), Instant.parse("2026-09-25T09:00:00Z"), "ECB");

        when(exchangeRateService.getMostRecentRate())
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/rates/latest/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.quote").value("JPY"))
                .andExpect(jsonPath("$.rate").value(147.35));

        verify(exchangeRateService).getMostRecentRate();
    }

    // ---------------------------------------------------------------
    // Error cases
    // ---------------------------------------------------------------

    @Test
    void getLatestPair_rateNotFound_returns404ProblemDetail() throws Exception {

        when(exchangeRateService.getLatestRate("USD", "EUR"))
                .thenThrow(new RateNotFoundException("USD", "EUR"));

        mockMvc.perform(
                        get("/api/v1/rates/latest/pair")
                                .param("base", "USD")
                                .param("quote", "EUR")
                )
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Exchange rate not found"))
                .andExpect(jsonPath("$.detail")
                        .value("Rate not found for USD/EUR"))
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/latest/pair"))
                .andExpect(jsonPath("$.timestamp")
                        .exists());

        verify(exchangeRateService)
                .getLatestRate("USD", "EUR");
    }

    @Test
    void getLatestPair_invalidCurrencyPair_returns400() throws Exception {

        when(exchangeRateService.getLatestRate("USD", "USD"))
                .thenThrow(new InvalidCurrencyPairException("USD", "USD"));

        mockMvc.perform(
                        get("/api/v1/rates/latest/pair")
                                .param("base", "USD")
                                .param("quote", "USD")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Invalid currency pair"))
                .andExpect(jsonPath("$.detail")
                        .value("Currency pair is invalid: base and quote must differ, got 'USD' and 'USD'"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/latest/pair"));

        verify(exchangeRateService)
                .getLatestRate("USD", "USD");
    }

    @Test
    void getHistory_invalidDateRange_returns400() throws Exception {

        when(exchangeRateService.getExchangeRatesByCurrencyAndDateRange(any()))
                .thenThrow(new InvalidDateRangeException());

        mockMvc.perform(
                        get("/api/v1/rates/history")
                                .param("currency", "USD")
                                .param("start", "2026-09-20T00:00:00Z")
                                .param("end", "2026-09-10T00:00:00Z")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Invalid date range"))
                .andExpect(jsonPath("$.detail")
                        .value("Start date must be before end date"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/history"));

        verify(exchangeRateService)
                .getExchangeRatesByCurrencyAndDateRange(any());
    }

    @Test
    void getHistory_periodTooLarge_returns400() throws Exception {

        when(exchangeRateService.getExchangeRatesByCurrencyAndDateRange(any()))
                .thenThrow(new HistoryPeriodTooLargeException(365));

        mockMvc.perform(
                        get("/api/v1/rates/history")
                                .param("currency", "USD")
                                .param("start", "2024-01-01T00:00:00Z")
                                .param("end", "2026-09-01T00:00:00Z")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("History period too large"))
                .andExpect(jsonPath("$.detail")
                        .value("Maximum history period is 365 days"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/history"));

        verify(exchangeRateService)
                .getExchangeRatesByCurrencyAndDateRange(any());
    }

    @Test
    void getHistory_multipleInvalidFields_returns400WithErrorsMap() throws Exception {

        mockMvc.perform(
                        get("/api/v1/rates/history")
                                .param("currency", "US")      // invalid: must be exactly 3 chars
                                .param("size", "200")         // invalid: must be <= 100
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("One or more request fields are invalid."))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/history"))
                .andExpect(jsonPath("$.errors.currency").exists())
                .andExpect(jsonPath("$.errors.size").exists());

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void getLatestByBase_invalidLimit_returns400() throws Exception {

        mockMvc.perform(
                        get("/api/v1/rates/latest/base/USD")
                                .param("limit", "101")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.status")
                        .value(400));

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void getLatestByBase_invalidBase_returns400() throws Exception {

        mockMvc.perform(
                        get("/api/v1/rates/latest/base/US")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.status")
                        .value(400));

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void getLatestPair_unexpectedException_returns500() throws Exception {

        when(exchangeRateService.getLatestRate("USD", "EUR"))
                .thenThrow(new RuntimeException("database exploded"));

        mockMvc.perform(
                        get("/api/v1/rates/latest/pair")
                                .param("base", "USD")
                                .param("quote", "EUR")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Internal server error"))
                .andExpect(jsonPath("$.detail")
                        .value("Unexpected server error"))
                .andExpect(jsonPath("$.status")
                        .value(500))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/rates/latest/pair"));

        verify(exchangeRateService)
                .getLatestRate("USD", "EUR");
    }
}