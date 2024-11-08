package com.local.exchange_service.external.openexchangerates;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for interacting with the OpenExchangeRates API.
 *
 * These properties are populated from the application's configuration (e.g., application.properties or application.yml)
 * using the {@link ConfigurationProperties} annotation with the prefix "external.openexchangerates".
 */
@Component
@ConfigurationProperties(prefix = "external.openexchangerates")
public class OpenExchangeRatesProperties {
    private String baseUrl;

    private String baseCurrency;

    private String appId;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
