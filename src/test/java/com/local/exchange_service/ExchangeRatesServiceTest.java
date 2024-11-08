package com.local.exchange_service;

import com.local.exchange_service.interfaces.IExchangeApiService;
import com.local.exchange_service.interfaces.IExchangeLoggingService;
import com.local.exchange_service.interfaces.IExchangeRates;
import com.local.exchange_service.model.ExchangeRates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExchangeRatesServiceTest {

    @Mock
    private IExchangeApiService exchangeApi;

    @Mock
    private IExchangeLoggingService exchangeLoggingService;

    private ExchangeRatesService exchangeRatesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exchangeRatesService = new ExchangeRatesService(exchangeApi,
                exchangeLoggingService, 1000, 2);
    }

    @Test
    void testGetCurrencies() {

        // Test initial state
        assertEquals(Collections.emptySet(), exchangeRatesService.getCurrencies());

        // Test mocked state
        Map<String, IExchangeRates> exchangeRatesMap = Map.of("USD", mock(IExchangeRates.class),
                "EUR", mock(IExchangeRates.class));
        ReflectionTestUtils.setField(exchangeRatesService, "exchangeRatesMap", exchangeRatesMap);

        assertEquals(Set.of("USD", "EUR"), exchangeRatesService.getCurrencies());
    }

    @Test
    void testGetExchangeRates() {
        IExchangeRates latestResponse = mock(IExchangeRates.class);
        Map rates = mock(Map.class);
        when(latestResponse.rates()).thenReturn(rates);
        when(rates.isEmpty()).thenReturn(false);
        when(rates.containsKey(any())).thenReturn(true);

        ReflectionTestUtils.setField(exchangeRatesService, "latestResponse", latestResponse);

        IExchangeRates usdRates = mock(IExchangeRates.class);
        IExchangeRates eurRates = mock(IExchangeRates.class);
        Map<String, IExchangeRates> exchangeRatesMap = Map.of("USD", usdRates,
                "EUR", eurRates);

        ReflectionTestUtils.setField(exchangeRatesService, "exchangeRatesMap", exchangeRatesMap);

        assertEquals(usdRates, exchangeRatesService.getExchangeRates("USD"));
        assertEquals(eurRates, exchangeRatesService.getExchangeRates("EUR"));
        RuntimeException e = assertThrows(RuntimeException.class, () -> exchangeRatesService.getExchangeRates("UAH"));
        assertEquals("Currency code is not added to exchange service", e.getMessage());
    }

    @Test
    void testAddCurrencyAndUpdateRates() throws InterruptedException {
        IExchangeRates latestResponse = mock(IExchangeRates.class);
        when(latestResponse.timestamp()).thenReturn(12345L);
        when(latestResponse.baseCurrency()).thenReturn("USD");
        Map<String, BigDecimal> latestResponseRates = Map.of("USD", BigDecimal.valueOf(1),
                "EUR", BigDecimal.valueOf(0.93),
                "UAH", BigDecimal.valueOf(41.15),
                "GBP", BigDecimal.valueOf(0.77));
        when(latestResponse.rates()).thenReturn(latestResponseRates);

        ReflectionTestUtils.setField(exchangeRatesService, "latestResponse", latestResponse);

        Map<String, IExchangeRates> exchangeRatesMap = new HashMap<>();
        ReflectionTestUtils.setField(exchangeRatesService, "exchangeRatesMap", exchangeRatesMap);

        assertEquals(Set.of("USD"), exchangeRatesService.addCurrency("USD"));
        exchangeRatesMap = (Map<String, IExchangeRates>) ReflectionTestUtils.getField(exchangeRatesService,
                "exchangeRatesMap");
        assertEquals(new ExchangeRates(12345L,
                        "USD", Map.of("USD", BigDecimal.valueOf(1))),
                exchangeRatesMap.get("USD"));

        assertEquals(Set.of("USD", "EUR"), exchangeRatesService.addCurrency("EUR"));
        exchangeRatesMap = (Map<String, IExchangeRates>) ReflectionTestUtils.getField(exchangeRatesService,
                "exchangeRatesMap");
        assertEquals(new ExchangeRates(12345L,
                        "USD", Map.of("USD", BigDecimal.valueOf(1),
                        "EUR", BigDecimal.valueOf(0.93))),
                exchangeRatesMap.get("USD"));
        assertEquals(new ExchangeRates(12345L,
                        "EUR", Map.of("USD", BigDecimal.valueOf(1.08),
                        "EUR", BigDecimal.valueOf(1))),
                exchangeRatesMap.get("EUR"));
    }

    @Test
    void testFetchExchangeRatesEmptyCurrencies() {
        Map<String, IExchangeRates> exchangeRatesMap = new HashMap<>();
        ReflectionTestUtils.setField(exchangeRatesService, "exchangeRatesMap", exchangeRatesMap);

        IExchangeRates mockResponse = mock(IExchangeRates.class);
        when(exchangeApi.getExchangeRates()).thenReturn(mockResponse);

        exchangeRatesService.fetchExchangeRates();

        exchangeRatesMap = (Map<String, IExchangeRates>) ReflectionTestUtils.getField(exchangeRatesService,
                "exchangeRatesMap");
        assertTrue(exchangeRatesMap.isEmpty());
        verify(exchangeApi, times(1)).getExchangeRates();
        verify(exchangeLoggingService, times(1)).logCurrencyRate(mockResponse);
    }

    @Test
    void testFetchExchangeRatesAndUpdateRates() {
        Map<String, IExchangeRates> exchangeRatesMap = Map.of("USD", mock(IExchangeRates.class),
                "EUR", mock(IExchangeRates.class));
        ReflectionTestUtils.setField(exchangeRatesService, "exchangeRatesMap", exchangeRatesMap);

        IExchangeRates latestResponse = mock(IExchangeRates.class);
        when(latestResponse.timestamp()).thenReturn(123456L);
        when(latestResponse.baseCurrency()).thenReturn("USD");
        Map<String, BigDecimal> latestResponseRates = Map.of("USD", BigDecimal.valueOf(1),
                "EUR", BigDecimal.valueOf(0.94),
                "UAH", BigDecimal.valueOf(41.15),
                "GBP", BigDecimal.valueOf(0.77));
        when(latestResponse.rates()).thenReturn(latestResponseRates);
        when(exchangeApi.getExchangeRates()).thenReturn(latestResponse);

        exchangeRatesService.fetchExchangeRates();

        exchangeRatesMap = (Map<String, IExchangeRates>) ReflectionTestUtils.getField(exchangeRatesService,
                "exchangeRatesMap");
        assertEquals(new ExchangeRates(123456L,
                        "USD", Map.of("USD", BigDecimal.valueOf(1),
                        "EUR", BigDecimal.valueOf(0.94))),
                exchangeRatesMap.get("USD"));
        assertEquals(new ExchangeRates(123456L,
                        "EUR", Map.of("USD", BigDecimal.valueOf(1.06),
                        "EUR", BigDecimal.valueOf(1))),
                exchangeRatesMap.get("EUR"));
        verify(exchangeApi, times(1)).getExchangeRates();
        verify(exchangeLoggingService, times(1)).logCurrencyRate(latestResponse);
    }

    @Test
    void testValidateNotInitialized() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> exchangeRatesService.addCurrency("USD"));
        assertEquals("Exchange rates service is not initialized yet", e.getMessage());

        e = assertThrows(RuntimeException.class, () -> exchangeRatesService.getExchangeRates("USD"));
        assertEquals("Exchange rates service is not initialized yet", e.getMessage());
    }

    @Test
    void testValidateRatesUnavailable() {
        IExchangeRates mockResponse = mock(IExchangeRates.class);
        when(mockResponse.rates()).thenReturn(Map.of());
        ReflectionTestUtils.setField(exchangeRatesService, "latestResponse", mockResponse);

        RuntimeException e = assertThrows(RuntimeException.class, () -> exchangeRatesService.addCurrency("USD"));
        assertEquals("Exchange rates are unavailable", e.getMessage());

        e = assertThrows(RuntimeException.class, () -> exchangeRatesService.getExchangeRates("USD"));
        assertEquals("Exchange rates are unavailable", e.getMessage());
    }

    @Test
    void testValidateCodeNotSupported() {
        IExchangeRates mockResponse = mock(IExchangeRates.class);
        when(mockResponse.rates()).thenReturn(Map.of("EUR", BigDecimal.valueOf(1)));
        ReflectionTestUtils.setField(exchangeRatesService, "latestResponse", mockResponse);

        RuntimeException e = assertThrows(RuntimeException.class, () -> exchangeRatesService.addCurrency("USD"));
        assertEquals("Currency code is not supported", e.getMessage());

        e = assertThrows(RuntimeException.class, () -> exchangeRatesService.getExchangeRates("USD"));
        assertEquals("Currency code is not supported", e.getMessage());
    }
}

