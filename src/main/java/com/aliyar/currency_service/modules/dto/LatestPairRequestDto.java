package com.aliyar.currency_service.modules.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LatestPairRequestDto(

        @Schema(description = "Базовая валюта (ISO 4217)", example = "USD")
        @NotBlank
        @Size(min = 3, max = 3)
        String base,

        @Schema(description = "Котируемая валюта (ISO 4217)", example = "KZT")
        @NotBlank
        @Size(min = 3, max = 3)
        String quote
) {
}