package com.aliyar.currency_service.modules.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface CrossRatePoint {
    Instant getEffectiveAt();

    BigDecimal getRate();
}