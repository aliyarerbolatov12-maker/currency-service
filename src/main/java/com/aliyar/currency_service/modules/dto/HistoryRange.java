package com.aliyar.currency_service.modules.dto;

import org.springframework.data.domain.Pageable;

import java.time.Instant;

public record HistoryRange(Instant start, Instant end, int page, int size, Pageable pageable) {
}