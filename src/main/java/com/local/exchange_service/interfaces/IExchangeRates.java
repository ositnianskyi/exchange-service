package com.local.exchange_service.interfaces;

import java.math.BigDecimal;
import java.util.Map;

public interface IExchangeRates {

    long timestamp();

    String baseCurrency();

    Map<String, BigDecimal> rates();
}
