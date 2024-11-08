package com.local.exchange_service;

import com.local.exchange_service.model.ExchangeRates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeController.class)
public class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRatesService exchangeRatesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCurrencies() throws Exception {
        when(exchangeRatesService.getCurrencies()).thenReturn(Set.of("USD", "EUR", "UAH"));

        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", containsInAnyOrder("USD", "EUR", "UAH")));

        verify(exchangeRatesService, times(1)).getCurrencies();
    }

    @Test
    void testAddCurrency() throws Exception {
        when(exchangeRatesService.addCurrency("BTC")).thenReturn(Set.of("USD", "EUR", "UAH", "BTC"));

        mockMvc.perform(post("/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"BTC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", containsInAnyOrder("USD", "EUR", "UAH", "BTC")));

        verify(exchangeRatesService, times(1)).addCurrency("BTC");
    }

    @Test
    void testGetExchangeRates() throws Exception {
        ExchangeRates exchangeRates = new ExchangeRates(12345L, "USD",
                Map.of("EUR", BigDecimal.valueOf(0.93),
                        "UAH", BigDecimal.valueOf(41.5)));

        when(exchangeRatesService.getExchangeRates("USD")).thenReturn(exchangeRates);

        mockMvc.perform(get("/exchange-rates?base=USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value(12345L))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.rates.EUR").value(0.93))
                .andExpect(jsonPath("$.rates.UAH").value(41.5));

        verify(exchangeRatesService, times(1)).getExchangeRates("USD");
    }
}
