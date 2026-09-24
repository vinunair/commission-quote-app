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

    private static final String FAILURE_MESSAGE = "Unable to generate quote right now. Please try again.";

    public QuoteResponse requestQuote(LoanQuoteRequest request) {
        QuoteResponse quote;
        try {
            quote = vendorRestClient.post()
                    .uri("/vendor/commission-quote")
                    .header("api-key", vendorProperties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(QuoteResponse.class);
        } catch (RestClientException e) {
            throw new VendorUnavailableException(FAILURE_MESSAGE, e);
        }

        // A 2xx with an empty or partial body would otherwise reach the UI as a blank/$0.00 quote.
        if (quote == null || quote.quoteId() == null
                || quote.commissionRate() == null || quote.totalCommission() == null) {
            throw new VendorUnavailableException(FAILURE_MESSAGE,
                    new IllegalStateException("Vendor returned an incomplete quote: " + quote));
        }
        return quote;
    }
}
