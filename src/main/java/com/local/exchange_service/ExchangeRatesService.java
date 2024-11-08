package com.local.exchange_service;

import com.local.exchange_service.interfaces.IExchangeApiService;
import com.local.exchange_service.interfaces.IExchangeLoggingService;
import com.local.exchange_service.interfaces.IExchangeRates;
import com.local.exchange_service.model.ExchangeRates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service class for managing exchange rates.
 * This service interacts with an external API to fetch exchange rates and maintains an internal map of exchange rates
 * for supported currencies. It provides functionality to add new currencies, fetch exchange rates, and log the exchange
 * rates for auditing or other purposes.
 */
@Service
public class ExchangeRatesService {

    private final IExchangeApiService exchangeApi;
    private final IExchangeLoggingService exchangeLoggingService;
    private final long updateLockTimeout;
    private final int exchangeScale;
    private final ReentrantLock updateLock = new ReentrantLock(true);
    private volatile Map<String, IExchangeRates> exchangeRatesMap = new HashMap<>();
    private volatile IExchangeRates latestResponse;

    /**
     * Constructor for the ExchangeRatesService.
     *
     * @param exchangeApi             The external API service for fetching exchange rates.
     * @param exchangeLoggingService  The service for logging exchange rates.
     * @param updateLockTimeout       Timeout for acquiring a lock when updating exchange rates.
     * @param exchangeScale           The scale (precision) for exchange rate calculations.
     */
    @Autowired
    public ExchangeRatesService(IExchangeApiService exchangeApi,
                                IExchangeLoggingService exchangeLoggingService,
                                @Value("${exchange.update.timeout}") long updateLockTimeout,
                                @Value("${exchange.scale}") int exchangeScale) {
        this.exchangeApi = exchangeApi;
        this.exchangeLoggingService = exchangeLoggingService;
        this.updateLockTimeout = updateLockTimeout;
        this.exchangeScale = exchangeScale;
    }

    /**
     * Retrieves the set of currencies for which exchange rates are available.
     *
     * @return A set of currency codes.
     */
    public Set<String> getCurrencies() {
        return exchangeRatesMap.keySet();
    }

    /**
     * Adds a new currency to the exchange rates service.
     * The currency is added only if it is supported by the external API.
     *
     * @param currencyCode The currency code to add.
     * @return A set of all currencies, including the newly added one.
     * @throws RuntimeException if the currency is not supported or the operation times out.
     */
    public Set<String> addCurrency(final String currencyCode) {
        validate(currencyCode);

        try {
            if (updateLock.tryLock(updateLockTimeout, TimeUnit.MILLISECONDS)) {
                try {
                    var exchangeSet = new HashSet<>(exchangeRatesMap.keySet());
                    if (exchangeSet.add(currencyCode)) {
                        this.exchangeRatesMap = buildExchangeRatesMap(exchangeSet, latestResponse, exchangeScale);
                    }

                    return exchangeSet;
                } finally {
                    updateLock.unlock();
                }
            } else {
                throw new RuntimeException("Unable to add currency due to timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to add currency due to thread interruption");
        }
    }

    /**
     * Retrieves the exchange rates for a specific currency.
     *
     * @param currencyCode The currency code to fetch exchange rates for.
     * @return The exchange rates for the given currency.
     * @throws RuntimeException if the currency is not supported or not available.
     */
    public IExchangeRates getExchangeRates(final String currencyCode) {
        validate(currencyCode);

        IExchangeRates rates = exchangeRatesMap.get(currencyCode);
        if (rates == null) {
            throw new RuntimeException("Currency code is not added to exchange service");
        }

        return rates;
    }

    /**
     * Periodically fetches the latest exchange rates from the external API.
     * This method is annotated with @Retryable to retry up to 3 times in case of failure.
     * The schedule for this method is defined in the configuration property "update.exchange.scheduled.rate".
     */
    @Retryable(value = RuntimeException.class, maxAttempts = 3)
    @Scheduled(fixedRateString = "${update.exchange.scheduled.rate}")
    protected void fetchExchangeRates() {
        var response = exchangeApi.getExchangeRates();

        try {
            updateLock.lock();

            this.latestResponse = response;
            if (!CollectionUtils.isEmpty(exchangeRatesMap.keySet())) { // check is current currencies is not empty
                this.exchangeRatesMap = buildExchangeRatesMap(exchangeRatesMap.keySet(), latestResponse, exchangeScale);
            }
        } finally {
            updateLock.unlock();
        }

        exchangeLoggingService.logCurrencyRate(response);
    }

    /**
     * Checks if the exchange rates service has been initialized with valid exchange rates.
     *
     * @return True if the service is initialized with valid exchange rates; false otherwise.
     */
    protected boolean isInitialized() {
        return latestResponse != null;
    }

    /**
     * Validates if the currency code is supported and if the exchange rates are available.
     *
     * @param currencyCode The currency code to validate.
     * @throws RuntimeException if the service is not initialized or the currency is not supported.
     */
    protected void validate(final String currencyCode) {
        if (!isInitialized()) {
            throw new RuntimeException("Exchange rates service is not initialized yet");
        }

        Map<String, BigDecimal> rates = latestResponse.rates();
        if (rates == null || rates.isEmpty()) {
            throw new RuntimeException("Exchange rates are unavailable");
        }

        if (!rates.containsKey(currencyCode)) {
            throw new RuntimeException("Currency code is not supported");
        }
    }

    /**
     * Builds a map of exchange rates for the specified currencies.
     *
     * @param currencies      A set of currencies for which exchange rates are to be built.
     * @param exchangeRates   The latest exchange rates data.
     * @param exchangeScale   The scale (precision) for exchange rate calculations.
     * @return A map where the keys are currency codes and the values are the corresponding exchange rates.
     */
    private static Map<String, IExchangeRates> buildExchangeRatesMap(final Set<String> currencies,
                                                                     final IExchangeRates exchangeRates,
                                                                     final int exchangeScale) {
        Map<String, IExchangeRates> resultExchangeRatesMap = new HashMap<>();
        long timestamp = exchangeRates.timestamp();
        currencies.stream().forEach(currency -> resultExchangeRatesMap.put(currency,
                new ExchangeRates(timestamp, currency, buildExchangeRates(currency,
                        currencies, exchangeRates, exchangeScale))));

        return resultExchangeRatesMap;
    }

    /**
     * Builds the exchange rates for a given base currency and a set of target currencies.
     *
     * @param baseCurrency    The base currency for the exchange rate calculation.
     * @param currencies      A set of target currencies to calculate the exchange rates for.
     * @param exchangeRates   The latest exchange rates data.
     * @param exchangeScale   The scale (precision) for exchange rate calculations.
     * @return A map where the keys are currency codes and the values are the corresponding exchange rates.
     */
    private static Map<String, BigDecimal> buildExchangeRates(final String baseCurrency,
                                                              final Set<String> currencies,
                                                              final IExchangeRates exchangeRates,
                                                              final int exchangeScale) {
        Map<String, BigDecimal> rates = new HashMap<>();
        Map<String, BigDecimal> latestRates = exchangeRates.rates();
        BigDecimal baseCurrencyRate = latestRates.get(baseCurrency);
        currencies.stream().forEach(currency -> {
            var currencyRate = latestRates.get(currency);
            BigDecimal value = currencyRate.divide(baseCurrencyRate, exchangeScale, RoundingMode.HALF_UP);
            rates.put(currency, value.stripTrailingZeros());
        });

        return rates;
    }
}
