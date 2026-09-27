package com.aliyar.currency_service.client;

import com.aliyar.currency_service.client.dto.CurrencyResponse;
import com.aliyar.currency_service.client.dto.ExchangeRateResponse;
import com.aliyar.currency_service.client.dto.RateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "frankfurter-client", url = "https://api.frankfurter.dev/v2")
public interface FrankfurterClient {

    @GetMapping("/rates")
    List<RateResponse> getLatestRates(@RequestParam("base") String base);

    @GetMapping("/rate/{base}/{quote}")
    ExchangeRateResponse getSpecificRate(
            @PathVariable String base,
            @PathVariable String quote
    );

    @GetMapping("/currencies")
    List<CurrencyResponse> getSupportedCurrencies();
}