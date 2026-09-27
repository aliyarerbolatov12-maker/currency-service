package com.aliyar.currency_service.modules.mapper;

import com.aliyar.currency_service.modules.dto.ExchangeRateResponseDto;
import com.aliyar.currency_service.modules.entity.ExchangeRate;
import com.aliyar.currency_service.provider.dto.ExchangeRateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExchangeRateMapper {

    @Mapping(target = "id", ignore = true)
    ExchangeRate toEntity(ExchangeRateDto dto);

    List<ExchangeRate> toEntityListFromDto(List<ExchangeRateDto> rates);

    ExchangeRateResponseDto toDto(ExchangeRateDto dto);

    List<ExchangeRateResponseDto> toResponseDtoListFromDto(List<ExchangeRateDto> rates);

    ExchangeRateResponseDto toDto(ExchangeRate entity);

    List<ExchangeRateResponseDto> toResponseDtoListFromEntity(List<ExchangeRate> entities);
}