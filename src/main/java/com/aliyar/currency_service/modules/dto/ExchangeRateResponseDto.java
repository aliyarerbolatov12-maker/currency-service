package com.aliyar.currency_service.modules.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Курс валютной пары")
public record ExchangeRateResponseDto(

        @Schema(description = "ID записи (null для рассчитанных курсов)", example = "42", nullable = true)
        Long id,

        @Schema(description = "Базовая валюта", example = "USD")
        String base,

        @Schema(description = "Котируемая валюта", example = "KZT")
        String quote,

        @Schema(description = "Сколько единиц quote за 1 base", example = "512.345678")
        BigDecimal rate,

        @Schema(description = "Момент, на который действует курс", example = "2026-09-29T10:00:00Z")
        Instant effectiveAt,

        @Schema(description = "Источник курса; 'computed' — рассчитан через USD", example = "computed")
        String providerName
) {
}