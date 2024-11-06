package com.local.exchange_service;

import com.local.exchange_service.interfaces.IExchangeRates;
import com.local.exchange_service.model.CurrencyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
public class ExchangeController {

    private final ExchangeRatesService exchangeRatesService;

    @Autowired
    public ExchangeController(ExchangeRatesService exchangeRatesService) {
        this.exchangeRatesService = exchangeRatesService;
    }

    @GetMapping("/currencies")
    public Set<String> getCurrencies() {
        return exchangeRatesService.getCurrencies();
    }

    @PostMapping("/currencies")
    public ResponseEntity<Set<String>> addCurrency(@RequestBody CurrencyRequest currencyRequest) {
        var currencies = exchangeRatesService.addCurrency(currencyRequest.code());
        return ResponseEntity.status(HttpStatus.CREATED).body(currencies);
    }

    @GetMapping("/exchange-rates")
    public IExchangeRates getExchangeRates(@RequestParam(value = "base", defaultValue = "USD") String base) {
        return exchangeRatesService.getExchangeRates(base);
    }
}
