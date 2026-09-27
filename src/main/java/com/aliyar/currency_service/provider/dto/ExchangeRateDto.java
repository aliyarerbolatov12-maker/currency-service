package com.aliyar.currency_service.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateDto(
        @NotBlank @Size(min = 3, max = 3) String base,
        @NotBlank @Size(min = 3, max = 3) String quote,
        @Positive BigDecimal rate,
        @NotNull Instant effectiveAt,
        @NotBlank String providerName
) {
}