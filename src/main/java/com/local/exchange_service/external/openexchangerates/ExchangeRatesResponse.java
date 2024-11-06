package com.local.exchange_service.external.openexchangerates;

import com.local.exchange_service.interfaces.IExchangeRates;

import java.math.BigDecimal;
import java.util.Map;

public record ExchangeRatesResponse(
        long timestamp,
        String base,
        Map<String, BigDecimal> rates
) implements IExchangeRates {

    @Override
    public String baseCurrency() {
        return base;
    }
}
