package com.aliyar.currency_service.provider.mapper;

import com.aliyar.currency_service.client.dto.ExchangeRateResponse;
import com.aliyar.currency_service.client.dto.RateResponse;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FrankfurterMapper {

    @Mapping(target = "providerName", constant = "FRANKFURTER")
    @Mapping(target = "effectiveAt",
            expression = "java(response.date().atStartOfDay(java.time.ZoneOffset.UTC).toInstant())")
    ExchangeRateDto toDto(RateResponse response);

    @Mapping(target = "providerName", constant = "FRANKFURTER")
    @Mapping(target = "effectiveAt",
            expression = "java(response.date().atStartOfDay(java.time.ZoneOffset.UTC).toInstant())")
    ExchangeRateDto toDto(ExchangeRateResponse response);
}