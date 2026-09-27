package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.modules.dto.CrossRateHistoryRequestDto;
import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.dto.HistoryRange;
import com.aliyar.currency_service.modules.dto.HistoryRequestDto;
import com.aliyar.currency_service.modules.entity.ExchangeRate;
import com.aliyar.currency_service.modules.exception.*;
import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.CrossRatePoint;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import com.aliyar.currency_service.provider.ExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final long MAX_HISTORY_DAYS = 365;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String BRIDGE_CURRENCY = "USD";

    private final List<ExchangeRateProvider> providers;
    private final ExchangeRateWriteService writeService;
    private final ExchangeRateRepository repository;
    private final ExchangeRateMapper mapper;
    private final ExchangeRateReaderService readerService;

    public void updateAllRates() {
        log.info("Starting exchange rates update for all enabled providers...");

        for (var provider : providers) {
            try {
                updateProvider(provider.getProviderName());
            } catch (Exception ex) {
                log.error("Failed to update provider '{}'", provider.getProviderName(), ex);
            }
        }
    }

    public void updateProvider(String providerName) {
        log.info("Updating exchange rates for provider '{}'", providerName);

        var provider = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new ProviderNotFoundException(providerName));

        var rates = provider.fetchRates("USD");

        writeService.saveRates(rates);

        log.info("Exchange rates updated successfully for provider '{}'", providerName);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getExchangeRatesByCurrencyAndDateRange(
            HistoryRequestDto request) {

        HistoryRange range = resolveHistoryRange(
                request.start(), request.end(), request.page(), request.size());

        log.debug(
                "Getting rates for currency '{}' in range {} - {}, page={}, size={}",
                request.currency(), range.start(), range.end(), range.page(), range.size()
        );

        List<ExchangeRate> entities =
                repository.findExchangeRatesByCurrencyAndDateRange(
                        request.currency(),
                        range.start(),
                        range.end(),
                        range.pageable()
                );

        return mapper.toResponseDtoListFromEntity(entities);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getCrossRateHistory(CrossRateHistoryRequestDto request) {

        if (request.base().equalsIgnoreCase(request.quote())) {
            throw new InvalidCurrencyPairException(request.base(), request.quote());
        }

        HistoryRange range = resolveHistoryRange(
                request.start(), request.end(), request.page(), request.size());

        log.debug(
                "Getting cross-rate history for {}/{} in range {} - {}, page={}, size={}",
                request.base(), request.quote(), range.start(), range.end(), range.page(), range.size()
        );

        List<CrossRatePoint> points = repository.findCrossRateHistory(
                request.base(), request.quote(), BRIDGE_CURRENCY, range.start(), range.end(), range.pageable());

        return points.stream()
                .map(p -> new ExchangeRateResponseDto(
                        null, request.base(), request.quote(), p.getRate(), p.getEffectiveAt(), "computed"))
                .toList();
    }

    private HistoryRange resolveHistoryRange(
            Instant startParam, Instant endParam, Integer pageParam, Integer sizeParam) {

        Instant end = endParam != null ? endParam : Instant.now();
        Instant start = startParam != null ? startParam : end.minus(30, ChronoUnit.DAYS);

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException();
        }

        if (Duration.between(start, end).toDays() > MAX_HISTORY_DAYS) {
            throw new HistoryPeriodTooLargeException(MAX_HISTORY_DAYS);
        }

        int page = pageParam != null ? pageParam : DEFAULT_PAGE;
        int size = sizeParam != null ? sizeParam : DEFAULT_SIZE;

        return new HistoryRange(start, end, page, size, PageRequest.of(page, size));
    }

    public ExchangeRateResponseDto getLatestRate(String base, String quote) {
        return getCrossRate(base, quote);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getLatestRatesByBase(
            String base,
            int limit) {

        log.debug(
                "Getting latest rates for base '{}' with limit {}",
                base,
                limit
        );

        List<ExchangeRate> entities =
                repository.findByBaseOrderByEffectiveAtDesc(
                        base,
                        PageRequest.of(0, limit)
                );

        return mapper.toResponseDtoListFromEntity(entities);
    }

    public ExchangeRateResponseDto getMostRecentRate() {
        return readerService.getMostRecentRate();
    }

    public ExchangeRateResponseDto getCrossRate(String base, String quote) {

        if (base.equalsIgnoreCase(quote)) {
            return new ExchangeRateResponseDto(null, base, quote, BigDecimal.ONE, Instant.now(), "computed");
        }

        try {
            return readerService.getDirectRate(base, quote);
        } catch (RateNotFoundException directNotFound) {
            log.debug("Direct pair {}/{} not found, computing cross rate via {}", base, quote, BRIDGE_CURRENCY);
        }

        ExchangeRateResponseDto baseToUsd = readerService.getDirectRate(base, BRIDGE_CURRENCY);
        ExchangeRateResponseDto quoteToUsd = readerService.getDirectRate(quote, BRIDGE_CURRENCY);

        BigDecimal crossRate = baseToUsd.rate().divide(quoteToUsd.rate(), 6, RoundingMode.HALF_UP);

        Instant effectiveAt = baseToUsd.effectiveAt().isBefore(quoteToUsd.effectiveAt())
                ? baseToUsd.effectiveAt()
                : quoteToUsd.effectiveAt();

        return new ExchangeRateResponseDto(null, base, quote, crossRate, effectiveAt, "computed");
    }

}