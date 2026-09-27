package com.aliyar.currency_service.scheduler;

import com.aliyar.currency_service.config.ExchangeRateProperties;
import com.aliyar.currency_service.modules.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DynamicExchangeRateScheduler implements SchedulingConfigurer {
    private final ExchangeRateService service;
    private final ExchangeRateProperties properties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        properties.getProviders().forEach((name, config) -> {
            if (!config.isEnabled()) return;
            registrar.addCronTask(() -> service.updateProvider(name), config.getCron());
        });
    }
}