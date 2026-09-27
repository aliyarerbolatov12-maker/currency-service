package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.entity.ExchangeRate;
import com.aliyar.currency_service.modules.exception.RateNotFoundException;
import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateReaderServiceTest {

    @Mock
    ExchangeRateRepository repository;

    @Mock
    ExchangeRateMapper mapper;

    ExchangeRateReaderService readerService;

    @BeforeEach
    void setUp() {
        readerService = new ExchangeRateReaderService(repository, mapper);
    }

    @Test
    void getDirectRate_found_returnsMappedDto() {

        ExchangeRate entity = new ExchangeRate();
        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                1L, "USD", "EUR", new BigDecimal("0.92"), Instant.now(), "CBR");

        when(repository.findFirstByBaseAndQuoteOrderByEffectiveAtDesc("USD", "EUR"))
                .thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        ExchangeRateResponseDto result = readerService.getDirectRate("USD", "EUR");

        assertThat(result).isEqualTo(dto);
        verify(repository).findFirstByBaseAndQuoteOrderByEffectiveAtDesc("USD", "EUR");
        verify(mapper).toDto(entity);
    }

    @Test
    void getDirectRate_notFound_throwsRateNotFoundException() {

        when(repository.findFirstByBaseAndQuoteOrderByEffectiveAtDesc("USD", "EUR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.getDirectRate("USD", "EUR"))
                .isInstanceOf(RateNotFoundException.class);

        verifyNoInteractions(mapper);
    }

    @Test
    void getMostRecentRate_found_returnsMappedDto() {

        ExchangeRate entity = new ExchangeRate();
        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                2L, "USD", "JPY", new BigDecimal("147.5"), Instant.now(), "CBR");

        when(repository.findFirstByOrderByEffectiveAtDesc())
                .thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        ExchangeRateResponseDto result = readerService.getMostRecentRate();

        assertThat(result).isEqualTo(dto);
        verify(repository).findFirstByOrderByEffectiveAtDesc();
        verify(mapper).toDto(entity);
    }

    @Test
    void getMostRecentRate_notFound_throwsRateNotFoundException() {

        when(repository.findFirstByOrderByEffectiveAtDesc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.getMostRecentRate())
                .isInstanceOf(RateNotFoundException.class);

        verifyNoInteractions(mapper);
    }
}