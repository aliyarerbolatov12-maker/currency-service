package com.aliyar.currency_service.modules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CrossRateHistoryRequestDto(

        @Schema(description = "Базовая валюта (ISO 4217)", example = "EUR")
        @NotBlank
        @Size(min = 3, max = 3)
        String base,

        @Schema(description = "Котируемая валюта (ISO 4217), не равна base", example = "KZT")
        @NotBlank
        @Size(min = 3, max = 3)
        String quote,

        @Schema(description = "Начало периода (по умолчанию end - 30 дней)", example = "2026-09-01T00:00:00Z")
        Instant start,

        @Schema(description = "Конец периода (по умолчанию — сейчас)", example = "2026-09-29T00:00:00Z")
        Instant end,

        @Schema(description = "Номер страницы, с 0", example = "0", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Размер страницы", example = "20", defaultValue = "20")
        @Min(1)
        @Max(100)
        Integer size
) {
}