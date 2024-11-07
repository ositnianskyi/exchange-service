package com.local.exchange_service;

import com.local.exchange_service.interfaces.IExchangeApiService;
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

@Service
public class ExchangeRatesService {

    private final IExchangeApiService exchangeApi;
    private final long updateLockTimeout;
    private final int exchangeScale;
    private final ReentrantLock updateLock = new ReentrantLock(true);
    private volatile Map<String, IExchangeRates> exchangeRatesMap = new HashMap<>();
    private volatile IExchangeRates latestResponse;

    @Autowired
    public ExchangeRatesService(IExchangeApiService exchangeApi,
                                @Value("${exchange.update.timeout}") long updateLockTimeout,
                                @Value("${exchange.scale}") int exchangeScale) {
        this.exchangeApi = exchangeApi;
        this.updateLockTimeout = updateLockTimeout;
        this.exchangeScale = exchangeScale;
    }

    public Set<String> getCurrencies() {
        return exchangeRatesMap.keySet();
    }

    public Set<String> addCurrency(String currencyCode) {
        validate(currencyCode);

        try {
            if (updateLock.tryLock(updateLockTimeout, TimeUnit.MILLISECONDS)) {
                try {
                    var exchangeSet = new HashSet<>(exchangeRatesMap.keySet());
                    if (exchangeSet.add(currencyCode)) {
                        this.exchangeRatesMap = buildExchangeRatesMap(exchangeSet);
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

    public IExchangeRates getExchangeRates(String currencyCode) {
        validate(currencyCode);

        IExchangeRates rates = exchangeRatesMap.get(currencyCode);
        if (rates == null) {
            throw new RuntimeException("Currency code is not added to exchange service");
        }

        return rates;
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 3)
    @Scheduled(fixedRateString = "${update.exchange.scheduled.rate}")
    protected void fetchExchangeRates() {
        var response = exchangeApi.getExchangeRates();
        //TODO add log to DB here

        try {
            updateLock.lock();

            var latestResponseRates = Optional.ofNullable(latestResponse).map(IExchangeRates::rates);
            this.latestResponse = response;
            if (!latestResponseRates.equals(Optional.of(response.rates())) // check is rates updated
                    && !CollectionUtils.isEmpty(exchangeRatesMap.keySet()) // check is current currencies is not empty
            ) {
                this.exchangeRatesMap = buildExchangeRatesMap(exchangeRatesMap.keySet());
            }
        } finally {
            updateLock.unlock();
        }
    }

    protected Map<String, IExchangeRates> buildExchangeRatesMap(Set<String> currencies) {
        Map<String, IExchangeRates> resultExchangeRatesMap = new HashMap<>();
        long timestamp = latestResponse.timestamp();
        currencies.stream().forEach(currency -> resultExchangeRatesMap.put(currency,
                new ExchangeRates(timestamp, currency, buildExchangeRates(currency, currencies))));

        return resultExchangeRatesMap;
    }

    protected Map<String, BigDecimal> buildExchangeRates(String baseCurrency, Set<String> currencies) {
        Map<String, BigDecimal> rates = new HashMap<>();
        Map<String, BigDecimal> latestRates = latestResponse.rates();
        BigDecimal baseCurrencyRate = latestRates.get(baseCurrency);
        currencies.stream().forEach(currency -> {
            var currencyRate = latestRates.get(currency);
            BigDecimal value = currencyRate.divide(baseCurrencyRate, exchangeScale, RoundingMode.HALF_UP);
            rates.put(currency, value.stripTrailingZeros());
        });

        return rates;
    }

    protected void validate(String currencyCode) {
        if (latestResponse == null) {
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
}
