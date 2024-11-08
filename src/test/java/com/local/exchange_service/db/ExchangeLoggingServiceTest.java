package com.local.exchange_service.db;

import com.local.exchange_service.db.entities.CurrencyRate;
import com.local.exchange_service.db.entities.ExchangeRate;
import com.local.exchange_service.interfaces.IExchangeRates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class ExchangeLoggingServiceTest {

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    private ExchangeLoggingService exchangeLoggingService;

    @Autowired
    public void setExchangeLoggingService(CurrencyRateRepository currencyRateRepository) {
        this.exchangeLoggingService = new ExchangeLoggingService(currencyRateRepository);
    }

    @Test
    @Transactional
    public void testLogCurrencyRate() {
        IExchangeRates exchangeRates = mock(IExchangeRates.class);
        when(exchangeRates.timestamp()).thenReturn(12345L);
        when(exchangeRates.baseCurrency()).thenReturn("USD");
        Map<String, BigDecimal> rates = Map.of("USD", BigDecimal.valueOf(1),
                "EUR", BigDecimal.valueOf(0.93),
                "UAH", BigDecimal.valueOf(41.15));
        when(exchangeRates.rates()).thenReturn(rates);

        exchangeLoggingService.logCurrencyRate(exchangeRates);

        CurrencyRate savedCurrencyRate = currencyRateRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(savedCurrencyRate);
        assertEquals(exchangeRates.timestamp(), savedCurrencyRate.getTimestamp());
        assertEquals(exchangeRates.baseCurrency(), savedCurrencyRate.getBaseCurrency());

        Map<String, ExchangeRate> savedRates = savedCurrencyRate.getRates();
        assertEquals(BigDecimal.valueOf(1), savedRates.get("USD").getRate());
        assertEquals(BigDecimal.valueOf(0.93), savedRates.get("EUR").getRate());
        assertEquals(BigDecimal.valueOf(41.15), savedRates.get("UAH").getRate());
    }
}
