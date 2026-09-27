package com.aliyar.currency_service.modules.controller;

import com.aliyar.currency_service.modules.dto.CrossRateHistoryRequestDto;
import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.dto.HistoryRequestDto;
import com.aliyar.currency_service.modules.dto.LatestPairRequestDto;
import com.aliyar.currency_service.modules.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rates")
@RequiredArgsConstructor
@Validated
@ApiCommonErrors
@Tag(name = "Exchange Rates", description = "Курсы валют: обновление, история и актуальные значения")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @PostMapping("/update")
    @Operation(summary = "Обновить курсы у всех провайдеров",
            description = "Ошибка одного провайдера не прерывает обновление остальных")
    public void updateRates() {
        exchangeRateService.updateAllRates();
    }

    @GetMapping("/history")
    @Operation(summary = "История курсов по валюте",
            description = "По умолчанию последние 30 дней, максимум 365 дней")
    public List<ExchangeRateResponseDto> getHistory(
            @ParameterObject @Valid @ModelAttribute HistoryRequestDto request) {

        return exchangeRateService.getExchangeRatesByCurrencyAndDateRange(request);
    }

    @GetMapping("/history/cross")
    @Operation(summary = "История кросс-курса пары",
            description = "Курс считается через USD. Base и quote не должны совпадать")
    public List<ExchangeRateResponseDto> getCrossHistory(
            @ParameterObject @Valid @ModelAttribute CrossRateHistoryRequestDto request) {

        return exchangeRateService.getCrossRateHistory(request);
    }

    @GetMapping("/latest/pair")
    @Operation(summary = "Актуальный курс пары",
            description = "Прямой курс, а если его нет — кросс-курс через USD")
    @ApiResponse(responseCode = "404", description = "Курс для пары не найден",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ExchangeRateResponseDto getLatestPair(
            @ParameterObject @Valid @ModelAttribute LatestPairRequestDto request) {

        return exchangeRateService.getLatestRate(request.base(), request.quote());
    }

    @GetMapping("/latest/base/{base}")
    @Operation(summary = "Последние курсы по базовой валюте", description = "Новые записи сверху")
    public List<ExchangeRateResponseDto> getLatestByBase(
            @Parameter(description = "Код базовой валюты (ISO 4217)", example = "USD")
            @PathVariable @NotBlank @Size(min = 3, max = 3) String base,

            @Parameter(description = "Максимальное число записей", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        return exchangeRateService.getLatestRatesByBase(base, limit);
    }

    @GetMapping("/latest/recent")
    @Operation(summary = "Самая свежая запись курса", description = "Среди всех валют")
    @ApiResponse(responseCode = "404", description = "Курсов пока нет",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ExchangeRateResponseDto getMostRecent() {
        return exchangeRateService.getMostRecentRate();
    }
}