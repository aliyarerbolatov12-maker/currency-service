package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateWriteService {

    private final ExchangeRateRepository repository;
    private final ExchangeRateMapper mapper;

    @Transactional
    public void saveRates(List<ExchangeRateDto> rates) {
        mapper.toEntityListFromDto(rates).forEach(e ->
                repository.insertIfAbsent(e.getBase(), e.getQuote(), e.getRate(), e.getEffectiveAt(), e.getProviderName()));
    }
}