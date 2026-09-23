package com.example.commissionquote.controller;

import com.example.commissionquote.client.VendorClient;
import com.example.commissionquote.dto.QuoteResponse;
import com.example.commissionquote.exception.VendorUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
class QuoteControllerTest {

    private static final String VALID_REQUEST = """
            {"loanAmount": 10000, "loanTermInMonths": 36, "riskBand": "MEDIUM"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorClient vendorClient;

    @Test
    void returnsQuoteFromVendor() throws Exception {
        when(vendorClient.requestQuote(any()))
                .thenReturn(new QuoteResponse("q-123", new BigDecimal("0.025"), new BigDecimal("250.00")));

        mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").value("q-123"))
                .andExpect(jsonPath("$.commissionRate").value(0.025))
                .andExpect(jsonPath("$.totalCommission").value(250.00));
    }

    @Test
    void returnsBadGatewayWhenVendorFails() throws Exception {
        when(vendorClient.requestQuote(any()))
                .thenThrow(new VendorUnavailableException("Unable to generate quote right now. Please try again.", null));

        mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Unable to generate quote right now. Please try again."));
    }

    @Test
    void rejectsNonPositiveLoanAmount() throws Exception {
        mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loanAmount": -5, "loanTermInMonths": 36, "riskBand": "LOW"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("loanAmount must be greater than 0")));

        verify(vendorClient, never()).requestQuote(any());
    }

    @Test
    void rejectsOutOfRangeTermAndMissingRiskBand() throws Exception {
        mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loanAmount": 1000, "loanTermInMonths": 600}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("loanTermInMonths must be at most 480")))
                .andExpect(jsonPath("$.message", containsString("riskBand is required")));
    }

    @Test
    void rejectsUnknownRiskBand() throws Exception {
        mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loanAmount": 1000, "loanTermInMonths": 12, "riskBand": "EXTREME"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body or invalid field value"));
    }
}
