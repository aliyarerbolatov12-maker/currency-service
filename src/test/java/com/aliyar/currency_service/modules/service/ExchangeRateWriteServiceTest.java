package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.modules.entity.ExchangeRate;
import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateWriteServiceTest {

    @Mock
    ExchangeRateRepository repository;

    @Mock
    ExchangeRateMapper mapper;

    ExchangeRateWriteService writeService;

    @BeforeEach
    void setUp() {
        writeService = new ExchangeRateWriteService(repository, mapper);
    }

    @Test
    void saveRates_mapsDtosAndInsertsEntities() {

        ExchangeRateDto dto = new ExchangeRateDto(
                "USD",
                "EUR",
                new BigDecimal("0.92"),
                Instant.now(),
                "CBR"
        );

        List<ExchangeRateDto> dtos = List.of(dto);

        ExchangeRate entity = new ExchangeRate();

        entity.setBase("USD");
        entity.setQuote("EUR");
        entity.setRate(new BigDecimal("0.92"));
        entity.setEffectiveAt(dto.effectiveAt());
        entity.setProviderName("CBR");

        List<ExchangeRate> entities = List.of(entity);

        when(mapper.toEntityListFromDto(dtos))
                .thenReturn(entities);

        writeService.saveRates(dtos);

        verify(mapper)
                .toEntityListFromDto(dtos);

        verify(repository)
                .insertIfAbsent(
                        "USD",
                        "EUR",
                        new BigDecimal("0.92"),
                        dto.effectiveAt(),
                        "CBR"
                );
    }

    @Test
    void saveRates_emptyList_doesNotCallRepository() {

        when(mapper.toEntityListFromDto(List.of()))
                .thenReturn(List.of());

        writeService.saveRates(List.of());

        verify(mapper)
                .toEntityListFromDto(List.of());
    }
}