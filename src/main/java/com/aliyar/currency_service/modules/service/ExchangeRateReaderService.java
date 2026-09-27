package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.config.CacheNames;
import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.exception.RateNotFoundException;
import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateReaderService {

    private final ExchangeRateRepository repository;
    private final ExchangeRateMapper mapper;
    
    @Cacheable(
            value = CacheNames.EXCHANGE_RATE_LATEST,
            key = "#base + ':' + #quote",
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getDirectRate(String base, String quote) {

        log.debug("Cache miss for direct rate: {}/{}", base, quote);

        return repository.findFirstByBaseAndQuoteOrderByEffectiveAtDesc(base, quote)
                .map(mapper::toDto)
                .orElseThrow(() -> new RateNotFoundException(base, quote));
    }

    @Cacheable(
            value = CacheNames.EXCHANGE_RATE_MOST_RECENT,
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getMostRecentRate() {

        log.debug("Cache miss for most recent exchange rate");

        return repository.findFirstByOrderByEffectiveAtDesc()
                .map(mapper::toDto)
                .orElseThrow(RateNotFoundException::new);
    }
}