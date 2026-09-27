package com.aliyar.currency_service.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "exchange-rate")
public class ExchangeRateProperties {

    @Valid
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Getter
    @Setter
    public static class ProviderConfig {
        @NotBlank(message = "Cron expression must not be blank")
        private String cron;
        private boolean enabled = true;
    }
}