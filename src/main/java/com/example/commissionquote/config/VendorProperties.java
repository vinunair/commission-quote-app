package com.example.commissionquote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vendor")
public record VendorProperties(
        String apiKey,
        String baseUrl,
        int errorRatePercent,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
