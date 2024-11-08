package com.local.exchange_service.external.openexchangerates;

import com.local.exchange_service.interfaces.IExchangeRates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(OpenExchangeRatesApiService.class)
@Import(OpenExchangeRatesApiServiceTest.Config.class)
public class OpenExchangeRatesApiServiceTest {

    @Autowired
    private OpenExchangeRatesApiService openExchangeRatesApiService;

    private MockRestServiceServer server;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGetExchangeRatesSuccess() {
        String jsonResponse = """
                    {
                        "timestamp": 1730916000,
                        "base": "USD",
                        "rates": {
                            "USD": 1,
                            "EUR": 0.93,
                            "UAH": 41.5
                        }
                    }
                """;

        this.server.expect(requestTo("https://test.openexchangerates.org/api/latest.json?app_id=testAppId&base=USD"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        IExchangeRates exchangeRates = openExchangeRatesApiService.getExchangeRates();

        assertEquals(1730916000L, exchangeRates.timestamp());
        assertEquals("USD", exchangeRates.baseCurrency());
        assertEquals(Map.of("USD", BigDecimal.valueOf(1),
                        "EUR", BigDecimal.valueOf(0.93),
                        "UAH", BigDecimal.valueOf(41.5)),
                exchangeRates.rates());
    }

    @Test
    void testGetExchangeRatesServerError() {
        this.server.expect(requestTo("https://test.openexchangerates.org/api/latest.json?app_id=testAppId&base=USD"))
                .andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> openExchangeRatesApiService.getExchangeRates());
    }

    @TestConfiguration
    static class Config {
        @Bean
        public OpenExchangeRatesProperties openExchangeRatesProperties() {
            OpenExchangeRatesProperties properties = mock(OpenExchangeRatesProperties.class);
            when(properties.getBaseUrl()).thenReturn("https://test.openexchangerates.org/");
            when(properties.getAppId()).thenReturn("testAppId");
            when(properties.getBaseCurrency()).thenReturn("USD");
            return properties;
        }
    }
}
