package com.local.exchange_service.model;

import com.local.exchange_service.interfaces.IExchangeRates;

import java.math.BigDecimal;
import java.util.Map;

public record ExchangeRates(
        long timestamp,
        String baseCurrency,
        Map<String, BigDecimal> rates
) implements IExchangeRates {}
