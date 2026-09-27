package com.aliyar.currency_service.scheduler;

import com.aliyar.currency_service.config.ExchangeRateProperties;
import com.aliyar.currency_service.modules.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicExchangeRateSchedulerTest {

    @Mock
    ExchangeRateService service;

    @Mock
    ScheduledTaskRegistrar registrar;

    @Test
    void configureTasks_registersCronTaskOnlyForEnabledProviders() {

        Map<String, ExchangeRateProperties.ProviderConfig> providers = getStringProviderConfigMap();

        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setProviders(providers);

        DynamicExchangeRateScheduler scheduler = new DynamicExchangeRateScheduler(service, properties);

        scheduler.configureTasks(registrar);

        verify(registrar, times(1)).addCronTask(any(Runnable.class), eq("0 0 * * * *"));
        verify(registrar, never()).addCronTask(any(Runnable.class), eq("0 30 * * * *"));
    }

    private Map<String, ExchangeRateProperties.ProviderConfig> getStringProviderConfigMap() {
        ExchangeRateProperties.ProviderConfig enabledConfig = new ExchangeRateProperties.ProviderConfig();
        enabledConfig.setCron("0 0 * * * *");
        enabledConfig.setEnabled(true);

        ExchangeRateProperties.ProviderConfig disabledConfig = new ExchangeRateProperties.ProviderConfig();
        disabledConfig.setCron("0 30 * * * *");
        disabledConfig.setEnabled(false);

        Map<String, ExchangeRateProperties.ProviderConfig> providers = new LinkedHashMap<>();
        providers.put("CBR", enabledConfig);
        providers.put("FRANKFURTER", disabledConfig);
        return providers;
    }

    @Test
    void configureTasks_registeredTask_invokesUpdateProviderForCorrectName() {

        ExchangeRateProperties.ProviderConfig config = new ExchangeRateProperties.ProviderConfig();
        config.setCron("0 0 * * * *");
        config.setEnabled(true);

        Map<String, ExchangeRateProperties.ProviderConfig> providers = new LinkedHashMap<>();
        providers.put("CBR", config);

        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setProviders(providers);

        DynamicExchangeRateScheduler scheduler = new DynamicExchangeRateScheduler(service, properties);

        scheduler.configureTasks(registrar);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(registrar).addCronTask(taskCaptor.capture(), eq("0 0 * * * *"));

        taskCaptor.getValue().run();

        verify(service).updateProvider("CBR");
    }

    @Test
    void configureTasks_noProviders_registersNoTasks() {

        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setProviders(new LinkedHashMap<>());

        DynamicExchangeRateScheduler scheduler = new DynamicExchangeRateScheduler(service, properties);

        scheduler.configureTasks(registrar);

        verifyNoInteractions(registrar);
        verifyNoInteractions(service);
    }

    @Test
    void configureTasks_allProvidersDisabled_registersNoTasks() {

        ExchangeRateProperties.ProviderConfig disabledConfig = new ExchangeRateProperties.ProviderConfig();
        disabledConfig.setCron("0 0 * * * *");
        disabledConfig.setEnabled(false);

        Map<String, ExchangeRateProperties.ProviderConfig> providers = new LinkedHashMap<>();
        providers.put("CBR", disabledConfig);

        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setProviders(providers);

        DynamicExchangeRateScheduler scheduler = new DynamicExchangeRateScheduler(service, properties);

        scheduler.configureTasks(registrar);

        verifyNoInteractions(registrar);
    }
}