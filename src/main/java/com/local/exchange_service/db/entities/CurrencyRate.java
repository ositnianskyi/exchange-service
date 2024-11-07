package com.local.exchange_service.db.entities;

import com.local.exchange_service.interfaces.IExchangeRates;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;
@Entity
public class CurrencyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long timestamp;

    private String baseCurrency;

    @OneToMany(mappedBy = "currencyRate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "currency")
    private Map<String, ExchangeRate> rates = new HashMap<>();

    public CurrencyRate() {
    }

    public CurrencyRate(IExchangeRates exchangeRates) {
        this.timestamp = exchangeRates.timestamp();
        this.baseCurrency = exchangeRates.baseCurrency();
        this.rates = new HashMap<>();

        exchangeRates.rates()
                .forEach((currency, rateValue) ->
                        rates.put(currency, new ExchangeRate(currency, rateValue, this)));

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Map<String, ExchangeRate> getRates() {
        return rates;
    }

    public void setRates(Map<String, ExchangeRate> rates) {
        this.rates = rates;
    }
}
