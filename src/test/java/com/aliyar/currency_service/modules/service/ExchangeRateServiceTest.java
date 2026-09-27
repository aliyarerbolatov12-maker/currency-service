package com.aliyar.currency_service.modules.service;

import com.aliyar.currency_service.modules.dto.CrossRateHistoryRequestDto;
import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.dto.HistoryRequestDto;
import com.aliyar.currency_service.modules.entity.ExchangeRate;
import com.aliyar.currency_service.modules.exception.*;
import com.aliyar.currency_service.modules.mapper.ExchangeRateMapper;
import com.aliyar.currency_service.modules.repository.CrossRatePoint;
import com.aliyar.currency_service.modules.repository.ExchangeRateRepository;
import com.aliyar.currency_service.provider.ExchangeRateProvider;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import com.aliyar.currency_service.provider.exception.ProviderUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

    @Mock
    ExchangeRateRepository exchangeRateRepository;

    @Mock
    ExchangeRateMapper exchangeRateMapper;

    @Mock
    ExchangeRateProvider exchangeRateProvider;

    @Mock
    ExchangeRateReaderService readerService;
    ExchangeRateService exchangeRateService;
    @Mock
    private ExchangeRateWriteService writeService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(
                List.of(exchangeRateProvider),
                writeService,
                exchangeRateRepository,
                exchangeRateMapper,
                readerService
        );
    }

    @Test
    void getExchangeRates_startAfterEnd_throwsInvalidDateRange() {

        Instant end = Instant.now();
        Instant start = end.plus(1, ChronoUnit.DAYS);

        HistoryRequestDto requestDto = new HistoryRequestDto("USD", start, end, null, null);

        assertThatThrownBy(
                () -> exchangeRateService.getExchangeRatesByCurrencyAndDateRange(requestDto))
                .isInstanceOf(InvalidDateRangeException.class);

        verifyNoInteractions(exchangeRateMapper);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getExchangeRates_periodLongerThan365Days_throwsHistoryPeriodTooLarge() {
        Instant end = Instant.now();
        Instant start = end.minus(400, ChronoUnit.DAYS);

        HistoryRequestDto request = new HistoryRequestDto("USD", start, end, null, null);

        assertThatThrownBy(
                () -> exchangeRateService.getExchangeRatesByCurrencyAndDateRange(request))
                .isInstanceOf(HistoryPeriodTooLargeException.class);

        verifyNoInteractions(exchangeRateMapper);
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getExchangeRates_nullStartAndEnd_usesDefaults() {
        HistoryRequestDto historyRequestDto = new HistoryRequestDto("USD", null, null, null, null);

        List<ExchangeRate> entities = List.of(new ExchangeRate());

        List<ExchangeRateResponseDto> dtos = List.of(mock(ExchangeRateResponseDto.class));

        when(exchangeRateRepository.findExchangeRatesByCurrencyAndDateRange(
                eq("USD"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(entities);

        when(exchangeRateMapper.toResponseDtoListFromEntity(entities)).thenReturn(dtos);

        List<ExchangeRateResponseDto> result = exchangeRateService.getExchangeRatesByCurrencyAndDateRange(historyRequestDto);

        assertThat(result).isEqualTo(dtos);

        verify(exchangeRateRepository).findExchangeRatesByCurrencyAndDateRange(
                eq("USD"),
                any(Instant.class),
                any(Instant.class),
                eq(PageRequest.of(0, 20)));
    }

    @Test
    void getExchangeRates_customPageAndSize_passedToRepository() {
        Instant end = Instant.now();
        Instant start = end.minus(10, ChronoUnit.DAYS);
        HistoryRequestDto request = new HistoryRequestDto("EUR", start, end, 2, 5);

        when(exchangeRateRepository.findExchangeRatesByCurrencyAndDateRange(
                eq("EUR"), eq(start), eq(end), eq(PageRequest.of(2, 5))))
                .thenReturn(List.of());
        when(exchangeRateMapper.toResponseDtoListFromEntity(List.of())).thenReturn(List.of());

        exchangeRateService.getExchangeRatesByCurrencyAndDateRange(request);

        verify(exchangeRateRepository).findExchangeRatesByCurrencyAndDateRange(
                eq("EUR"), eq(start), eq(end), eq(PageRequest.of(2, 5)));
    }

    @Test
    void getLatestRate_directPairFound_returnsDtoWithoutComputing() {
        ExchangeRateResponseDto dto = new ExchangeRateResponseDto(
                1L, "USD", "EUR", new BigDecimal("0.9"), Instant.now(), "CBR");

        when(readerService.getDirectRate("USD", "EUR")).thenReturn(dto);

        ExchangeRateResponseDto result = exchangeRateService.getLatestRate("USD", "EUR");

        assertThat(result).isEqualTo(dto);
        verify(readerService, never()).getDirectRate("USD", "USD");
        verify(readerService, never()).getDirectRate("EUR", "USD");
    }

    @Test
    void getCrossRate_sameCurrency_returnsRateOfOneWithoutTouchingReader() {
        ExchangeRateResponseDto result = exchangeRateService.getCrossRate("USD", "USD");

        assertThat(result.rate()).isEqualByComparingTo("1");
        assertThat(result.base()).isEqualTo("USD");
        assertThat(result.quote()).isEqualTo("USD");
        verifyNoInteractions(readerService);
    }

    @Test
    void getCrossRate_noDirectPair_computesViaBridgeCurrency() {
        Instant nowKzt = Instant.now();
        Instant nowRub = nowKzt.minusSeconds(60);

        ExchangeRateResponseDto kztUsd = new ExchangeRateResponseDto(
                1L, "KZT", "USD", new BigDecimal("450"), nowKzt, "CBR");
        ExchangeRateResponseDto rubUsd = new ExchangeRateResponseDto(
                2L, "RUB", "USD", new BigDecimal("90"), nowRub, "CBR");

        when(readerService.getDirectRate("KZT", "RUB"))
                .thenThrow(new RateNotFoundException("KZT", "RUB"));
        when(readerService.getDirectRate("KZT", "USD")).thenReturn(kztUsd);
        when(readerService.getDirectRate("RUB", "USD")).thenReturn(rubUsd);

        ExchangeRateResponseDto result = exchangeRateService.getCrossRate("KZT", "RUB");

        assertThat(result.rate()).isEqualByComparingTo("5");
        assertThat(result.base()).isEqualTo("KZT");
        assertThat(result.quote()).isEqualTo("RUB");
        assertThat(result.effectiveAt()).isEqualTo(nowRub);
    }

    @Test
    void getLatestRate_directPairNotFoundAndBridgeMissing_propagatesRateNotFound() {
        when(readerService.getDirectRate("KZT", "RUB"))
                .thenThrow(new RateNotFoundException("KZT", "RUB"));
        when(readerService.getDirectRate("KZT", "USD"))
                .thenThrow(new RateNotFoundException("KZT", "USD"));

        assertThatThrownBy(() -> exchangeRateService.getLatestRate("KZT", "RUB"))
                .isInstanceOf(RateNotFoundException.class);
    }


    @Test
    void getCrossRateHistory_sameCurrency_throwsInvalidCurrencyPair() {
        var request = new CrossRateHistoryRequestDto("USD", "USD", null, null, null, null);

        assertThatThrownBy(() -> exchangeRateService.getCrossRateHistory(request))
                .isInstanceOf(InvalidCurrencyPairException.class);

        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getCrossRateHistory_startAfterEnd_throwsInvalidDateRange() {
        Instant end = Instant.now();
        Instant start = end.plus(1, ChronoUnit.DAYS);

        var request = new CrossRateHistoryRequestDto("KZT", "RUB", start, end, null, null);

        assertThatThrownBy(() -> exchangeRateService.getCrossRateHistory(request))
                .isInstanceOf(InvalidDateRangeException.class);

        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getCrossRateHistory_periodLongerThan365Days_throwsHistoryPeriodTooLarge() {
        Instant end = Instant.now();
        Instant start = end.minus(400, ChronoUnit.DAYS);

        var request = new CrossRateHistoryRequestDto("KZT", "RUB", start, end, null, null);

        assertThatThrownBy(() -> exchangeRateService.getCrossRateHistory(request))
                .isInstanceOf(HistoryPeriodTooLargeException.class);

        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void getCrossRateHistory_success_mapsPointsToDto() {
        var request = new CrossRateHistoryRequestDto("KZT", "RUB", null, null, null, null);

        CrossRatePoint point = mock(CrossRatePoint.class);
        Instant effectiveAt = Instant.now();
        when(point.getRate()).thenReturn(new BigDecimal("5.1"));
        when(point.getEffectiveAt()).thenReturn(effectiveAt);

        when(exchangeRateRepository.findCrossRateHistory(
                eq("KZT"), eq("RUB"), eq("USD"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(point));

        List<ExchangeRateResponseDto> result = exchangeRateService.getCrossRateHistory(request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().base()).isEqualTo("KZT");
        assertThat(result.getFirst().quote()).isEqualTo("RUB");
        assertThat(result.getFirst().rate()).isEqualByComparingTo("5.1");
        assertThat(result.getFirst().effectiveAt()).isEqualTo(effectiveAt);
    }

    @Test
    void getCrossRateHistory_customPageAndSize_passedToRepository() {
        Instant end = Instant.now();
        Instant start = end.minus(10, ChronoUnit.DAYS);
        var request = new CrossRateHistoryRequestDto("KZT", "RUB", start, end, 2, 5);

        when(exchangeRateRepository.findCrossRateHistory(
                eq("KZT"), eq("RUB"), eq("USD"), eq(start), eq(end), eq(PageRequest.of(2, 5))))
                .thenReturn(List.of());

        exchangeRateService.getCrossRateHistory(request);

        verify(exchangeRateRepository).findCrossRateHistory(
                eq("KZT"), eq("RUB"), eq("USD"), eq(start), eq(end), eq(PageRequest.of(2, 5)));
    }

    @Test
    void getLatestRatesByBase_passesCorrectPageable() {
        List<ExchangeRate> entities = List.of(new ExchangeRate());
        List<ExchangeRateResponseDto> dtos = List.of(mock(ExchangeRateResponseDto.class));

        when(exchangeRateRepository.findByBaseOrderByEffectiveAtDesc(eq("USD"), eq(PageRequest.of(0, 5))))
                .thenReturn(entities);
        when(exchangeRateMapper.toResponseDtoListFromEntity(entities)).thenReturn(dtos);

        List<ExchangeRateResponseDto> result = exchangeRateService.getLatestRatesByBase("USD", 5);

        assertThat(result).isEqualTo(dtos);
        verify(exchangeRateRepository).findByBaseOrderByEffectiveAtDesc("USD", PageRequest.of(0, 5));
    }

    @Test
    void getMostRecentRate_delegatesToReaderService() {
        ExchangeRateResponseDto dto = mock(ExchangeRateResponseDto.class);
        when(readerService.getMostRecentRate()).thenReturn(dto);

        ExchangeRateResponseDto result = exchangeRateService.getMostRecentRate();

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateProvider_found_fetchesAndSavesRates() {
        when(exchangeRateProvider.getProviderName()).thenReturn("CBR");
        List<ExchangeRateDto> rates = List.of(mock(ExchangeRateDto.class));
        when(exchangeRateProvider.fetchRates("USD")).thenReturn(rates);

        exchangeRateService.updateProvider("cbr");

        verify(exchangeRateProvider).fetchRates("USD");
        verify(writeService).saveRates(rates);
    }

    @Test
    void updateProvider_notFound_throwsProviderNotFound() {
        when(exchangeRateProvider.getProviderName()).thenReturn("CBR");

        assertThatThrownBy(() -> exchangeRateService.updateProvider("unknown"))
                .isInstanceOf(ProviderNotFoundException.class);

        verifyNoInteractions(writeService);
    }

    @Test
    void updateAllRates_providerUnavailable_isSwallowedSilently() {
        when(exchangeRateProvider.getProviderName()).thenReturn("CBR");
        when(exchangeRateProvider.fetchRates("USD")).thenThrow(new ProviderUnavailableException("CBR", null));

        exchangeRateService.updateAllRates();

        verify(exchangeRateProvider).fetchRates("USD");
        verifyNoInteractions(writeService);
    }

    @Test
    void updateAllRates_success_savesRates() {
        when(exchangeRateProvider.getProviderName()).thenReturn("CBR");
        List<ExchangeRateDto> rates = List.of(mock(ExchangeRateDto.class));
        when(exchangeRateProvider.fetchRates("USD")).thenReturn(rates);

        exchangeRateService.updateAllRates();

        verify(writeService).saveRates(rates);
    }
}