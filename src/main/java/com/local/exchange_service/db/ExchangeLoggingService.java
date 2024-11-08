package com.local.exchange_service.db;

import com.local.exchange_service.db.entities.CurrencyRate;
import com.local.exchange_service.interfaces.IExchangeLoggingService;
import com.local.exchange_service.interfaces.IExchangeRates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for logging currency exchange rates.
 */
@Service
public class ExchangeLoggingService implements IExchangeLoggingService {

    private CurrencyRateRepository currencyRateRepository;

    /**
     * Constructor to initialize the service with the {@link CurrencyRateRepository}.
     *
     * @param currencyRateRepository The repository responsible for persisting exchange rates.
     */
    @Autowired
    public ExchangeLoggingService(CurrencyRateRepository currencyRateRepository) {
        this.currencyRateRepository = currencyRateRepository;
    }

    /**
     * Logs the provided exchange rates to the database.
     * This method creates a new {@link CurrencyRate} entity from the provided {@link IExchangeRates}
     * and saves it to the {@link CurrencyRateRepository}.
     * The operation is performed within a transaction to ensure consistency.
     *
     * @param exchangeRates The exchange rates to log. This object should contain the exchange rates
     *                      to be persisted in the database.
     * @throws RuntimeException If there is an issue saving the exchange rates to the repository.
     */
    @Transactional
    public void logCurrencyRate(final IExchangeRates exchangeRates) {
        currencyRateRepository.save(new CurrencyRate(exchangeRates));
    }
}
