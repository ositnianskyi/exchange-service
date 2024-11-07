package com.local.exchange_service.db;

import com.local.exchange_service.db.entities.CurrencyRate;
import com.local.exchange_service.interfaces.IExchangeLoggingService;
import com.local.exchange_service.interfaces.IExchangeRates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeLoggingService implements IExchangeLoggingService {

    private CurrencyRateRepository currencyRateRepository;

    @Autowired
    public ExchangeLoggingService(CurrencyRateRepository currencyRateRepository) {
        this.currencyRateRepository = currencyRateRepository;
    }

    @Transactional
    public void logCurrencyRate(IExchangeRates exchangeRates) {
        currencyRateRepository.save(new CurrencyRate(exchangeRates));
    }
}
