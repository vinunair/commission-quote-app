package com.example.commissionquote.client;

import com.example.commissionquote.config.VendorProperties;
import com.example.commissionquote.dto.LoanQuoteRequest;
import com.example.commissionquote.dto.QuoteResponse;
import com.example.commissionquote.dto.RiskBand;
import com.example.commissionquote.exception.VendorUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VendorClientTest {

    private static final String VENDOR_URL = "http://vendor.test/vendor/commission-quote";
    private static final LoanQuoteRequest REQUEST =
            new LoanQuoteRequest(new BigDecimal("10000"), 36, RiskBand.LOW);

    private MockRestServiceServer vendorServer;
    private VendorClient vendorClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://vendor.test");
        vendorServer = MockRestServiceServer.bindTo(builder).build();
        VendorProperties properties = new VendorProperties("test-key", "http://vendor.test", 0, 1000, 1000);
        vendorClient = new VendorClient(builder.build(), properties);
    }

    @Test
    void sendsApiKeyAndReturnsQuote() {
        vendorServer.expect(requestTo(VENDOR_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andRespond(withSuccess("""
                        {"quoteId": "q-1", "commissionRate": 0.015, "totalCommission": 150.00}
                        """, MediaType.APPLICATION_JSON));

        QuoteResponse quote = vendorClient.requestQuote(REQUEST);

        assertThat(quote.quoteId()).isEqualTo("q-1");
        assertThat(quote.totalCommission()).isEqualByComparingTo("150.00");
        vendorServer.verify();
    }

    @Test
    void translatesVendorErrorStatus() {
        vendorServer.expect(requestTo(VENDOR_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> vendorClient.requestQuote(REQUEST))
                .isInstanceOf(VendorUnavailableException.class);
    }

    @Test
    void translatesTimeout() {
        vendorServer.expect(requestTo(VENDOR_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> vendorClient.requestQuote(REQUEST))
                .isInstanceOf(VendorUnavailableException.class);
    }
}
