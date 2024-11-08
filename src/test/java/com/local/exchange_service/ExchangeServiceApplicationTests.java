package com.local.exchange_service;

import com.local.exchange_service.db.CurrencyRateRepository;
import com.local.exchange_service.model.CurrencyRequest;
import com.local.exchange_service.model.ExchangeRates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ExchangeServiceApplicationTests {

    @Autowired
    CurrencyRateRepository rateRepository;
    @Autowired
    TestRestTemplate testRestTemplate;
    @Autowired
    ExchangeRatesService exchangeRatesService;
    @LocalServerPort
    private Integer port;
    private String baseUrl;

    @BeforeEach
    void setUp() throws InterruptedException {
        rateRepository.deleteAll();
        baseUrl = "http://localhost:" + port;
        waitInitialization();
    }

    @Test
    void testIntegration() {
        ResponseEntity<Set> getCurrencies = testRestTemplate.getForEntity(baseUrl + "/currencies", Set.class);
        assertTrue(getCurrencies.getStatusCode().is2xxSuccessful());
        assertTrue(getCurrencies.getBody().isEmpty());

        ResponseEntity<Set> addCurrencies = testRestTemplate.postForEntity(baseUrl + "/currencies",
                new CurrencyRequest("USD"), Set.class);
        assertEquals(HttpStatus.CREATED, addCurrencies.getStatusCode());
        assertEquals(Set.of("USD"), addCurrencies.getBody());

        ResponseEntity<ExchangeRates> getRates = testRestTemplate.getForEntity(baseUrl + "/exchange-rates?base=USD",
                ExchangeRates.class);
        assertTrue(getRates.getStatusCode().is2xxSuccessful());
        ExchangeRates exchangeRates = getRates.getBody();
        assertEquals("USD", exchangeRates.baseCurrency());
        assertEquals(BigDecimal.valueOf(1), exchangeRates.rates().get("USD"));

        addCurrencies = testRestTemplate.postForEntity(baseUrl + "/currencies",
                new CurrencyRequest("EUR"), Set.class);
        assertEquals(HttpStatus.CREATED, addCurrencies.getStatusCode());
        assertEquals(Set.of("USD", "EUR"), addCurrencies.getBody());

        getRates = testRestTemplate.getForEntity(baseUrl + "/exchange-rates?base=USD", ExchangeRates.class);
        assertTrue(getRates.getStatusCode().is2xxSuccessful());
        exchangeRates = getRates.getBody();
        assertEquals("USD", exchangeRates.baseCurrency());
        assertEquals(BigDecimal.valueOf(1), exchangeRates.rates().get("USD"));
        assertTrue(exchangeRates.rates().containsKey("EUR"));

        getRates = testRestTemplate.getForEntity(baseUrl + "/exchange-rates?base=EUR", ExchangeRates.class);
        assertTrue(getRates.getStatusCode().is2xxSuccessful());
        exchangeRates = getRates.getBody();
        assertEquals("EUR", exchangeRates.baseCurrency());
        assertEquals(BigDecimal.valueOf(1), exchangeRates.rates().get("EUR"));
        assertTrue(exchangeRates.rates().containsKey("USD"));
    }


    private void waitInitialization() throws InterruptedException {
        int retries = 10;
        while (!exchangeRatesService.isInitialized() && retries-- > 0) {
            Thread.sleep(1000);
        }
    }

}
