package com.local.exchange_service.db.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String currency;

    private BigDecimal rate;

    @ManyToOne
    @JoinColumn(name = "currency_rate_id")
    private CurrencyRate currencyRate;

    public ExchangeRate() {
    }

    public ExchangeRate(String currency, BigDecimal rate, CurrencyRate currencyRate) {
        this.currency = currency;
        this.rate = rate;
        this.currencyRate = currencyRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public CurrencyRate getCurrencyRate() {
        return currencyRate;
    }

    public void setCurrencyRate(CurrencyRate currencyRate) {
        this.currencyRate = currencyRate;
    }
}
