package com.example.commissionquote.client;

import com.example.commissionquote.config.VendorProperties;
import com.example.commissionquote.dto.LoanQuoteRequest;
import com.example.commissionquote.dto.QuoteResponse;
import com.example.commissionquote.exception.VendorUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VendorClient {

    private final RestClient vendorRestClient;
    private final VendorProperties vendorProperties;

    public VendorClient(RestClient vendorRestClient, VendorProperties vendorProperties) {
        this.vendorRestClient = vendorRestClient;
        this.vendorProperties = vendorProperties;
    }

    public QuoteResponse requestQuote(LoanQuoteRequest request) {
        try {
            return vendorRestClient.post()
                    .uri("/vendor/commission-quote")
                    .header("api-key", vendorProperties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(QuoteResponse.class);
        } catch (RestClientException e) {
            throw new VendorUnavailableException("Unable to generate quote right now. Please try again.", e);
        }
    }
}
