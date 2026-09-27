package com.aliyar.currency_service.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
        String base,
        String quote,
        BigDecimal rate,
        LocalDate date
) {
}