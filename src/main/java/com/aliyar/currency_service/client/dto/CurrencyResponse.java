package com.aliyar.currency_service.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrencyResponse(
        @JsonProperty("iso_code")
        String isoCode,
        @JsonProperty("iso_numeric")
        String isoNumeric,
        String name,
        String symbol
) {
}