package com.local.exchange_service.interfaces;

/**
 * Service interface for fetching exchange rates from an external API.
 */
public interface IExchangeApiService {

    /**
     * Retrieves the latest exchange rates.
     *
     * @return an {@link IExchangeRates} object containing exchange rate data.
     */
    IExchangeRates getExchangeRates();
}
