package com.local.exchange_service.external.openexchangerates;

import com.local.exchange_service.interfaces.IExchangeApiService;
import com.local.exchange_service.interfaces.IExchangeRates;
import com.local.exchange_service.model.ExchangeRates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.logging.Logger;

@Service
public class OpenExchangeRatesApiService implements IExchangeApiService {

    private static final Logger logger = Logger.getLogger(OpenExchangeRatesApiService.class.getName());


    private final RestTemplate restTemplate;

    private final UriComponents getLatestRateUrl;

    @Autowired
    public OpenExchangeRatesApiService(RestTemplate restTemplate, OpenExchangeRatesProperties properties) {
        this.restTemplate = restTemplate;
        this.getLatestRateUrl = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("api/latest.json")
                .queryParam("app_id", properties.getAppId())
                .queryParam("base", properties.getBaseCurrency())
                .build();
    }

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
