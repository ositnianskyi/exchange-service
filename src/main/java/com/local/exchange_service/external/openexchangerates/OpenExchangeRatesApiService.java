package com.local.exchange_service.external.openexchangerates;

import com.local.exchange_service.interfaces.IExchangeApiService;
import com.local.exchange_service.interfaces.IExchangeRates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.logging.Logger;

/**
 * Service class for interacting with the OpenExchangeRates API.
 * This service is responsible for fetching exchange rates from the OpenExchangeRates API using a RESTful call.
 * If the request is successful, it returns the exchange rates as an {@link IExchangeRates} object.
 */
@Service
public class OpenExchangeRatesApiService implements IExchangeApiService {

    private static final Logger logger = Logger.getLogger(OpenExchangeRatesApiService.class.getName());


    private final RestTemplate restTemplate;

    private final UriComponents getLatestRateUrl;

    /**
     * Constructor to initialize the service with required dependencies.
     *
     * @param restTemplate The {@link RestTemplate} used for making HTTP requests to the OpenExchangeRates API.
     * @param properties The {@link OpenExchangeRatesProperties} containing configuration like base URL,
     *                   base currency, and app ID for accessing the OpenExchangeRates API.
     */
    @Autowired
    public OpenExchangeRatesApiService(RestTemplate restTemplate, OpenExchangeRatesProperties properties) {
        this.restTemplate = restTemplate;
        this.getLatestRateUrl = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("api/latest.json")
                .queryParam("app_id", properties.getAppId())
                .queryParam("base", properties.getBaseCurrency())
                .build();
    }

    /**
     * Fetches the latest exchange rates from the OpenExchangeRates API.
     *
     * @return An {@link IExchangeRates} object containing the exchange rates data.
     * @throws RuntimeException If the request fails or the response is not successful.
     */
    public IExchangeRates getExchangeRates() {
        try {
            ResponseEntity<ExchangeRatesResponse> response = restTemplate.getForEntity(getLatestRateUrl.toUriString(), ExchangeRatesResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ExchangeRatesResponse body = response.getBody();
                logger.info("Fetched exchange rates:" + body);
                return body;
            } else {
                throw new RuntimeException("Unable to get exchange rates from " + getLatestRateUrl.getHost() +
                        " response code: " + response.getStatusCode());
            }
        } catch (RuntimeException e) {
            logger.severe("Unable to get exchange rates from " + getLatestRateUrl.getHost() +
                    " error message: " + e.getMessage());
            throw e;
        }
    }
}
