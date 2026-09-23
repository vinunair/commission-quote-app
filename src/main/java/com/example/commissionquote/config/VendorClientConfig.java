package com.example.commissionquote.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(VendorProperties.class)
public class VendorClientConfig {

    @Bean
    public RestClient vendorRestClient(VendorProperties vendorProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(vendorProperties.connectTimeoutMs());
        requestFactory.setReadTimeout(vendorProperties.readTimeoutMs());

        return RestClient.builder()
                .baseUrl(vendorProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
