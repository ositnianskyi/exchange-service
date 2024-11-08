package com.local.exchange_service.interfaces;

/**
 * Service interface for logging exchange rate information.
 */
public interface IExchangeLoggingService {

    /**
     * Logs the given exchange rate data to a persistent storage.
     *
     * @param exchangeRates an {@link IExchangeRates} object containing the exchange rates to be logged.
     */
    void logCurrencyRate(IExchangeRates exchangeRates);
}
