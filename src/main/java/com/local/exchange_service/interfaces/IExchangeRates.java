package com.local.exchange_service.interfaces;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface representing exchange rates data.
 */
public interface IExchangeRates {

    /**
     * Gets the timestamp of the exchange rates data.
     *
     * @return the timestamp of the exchange rates data.
     */
    long timestamp();

    /**
     * Gets the base currency of the exchange rates.
     *
     * @return the base currency of the exchange rates.
     */
    String baseCurrency();

    /**
     * Gets a map of currencies and their corresponding exchange rates against the base currency.
     *
     * @return a map with currency codes as keys and exchange rates as values.
     */
    Map<String, BigDecimal> rates();
}
