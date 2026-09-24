package com.example.commissionquote.controller;

import com.example.commissionquote.config.VendorProperties;
import com.example.commissionquote.service.VendorErrorSimulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorMockController.class)
@EnableConfigurationProperties(VendorProperties.class)
@TestPropertySource(properties = "vendor.api-key=test-key")
class VendorMockControllerTest {

    private static final String REQUEST = """
            {"loanAmount": 10000, "loanTermInMonths": 36, "riskBand": "HIGH"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorErrorSimulator errorSimulator;

    @ParameterizedTest
    @CsvSource({
            "1, 0.045, 450.00",
            "11, 0.045, 450.00",
            "12, 0.040, 400.00",
            "60, 0.040, 400.00",
            "61, 0.035, 350.00",
            "480, 0.035, 350.00"
    })
    void returnsQuoteWithTermAdjustment(int term, BigDecimal expectedRate, BigDecimal expectedTotal) throws Exception {
        when(errorSimulator.shouldFail()).thenReturn(false);

        String request = """
                {"loanAmount": 10000, "loanTermInMonths": %d, "riskBand": "HIGH"}
                """.formatted(term);

        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test-key")
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissionRate").value(expectedRate.doubleValue()))
                .andExpect(jsonPath("$.totalCommission").value(expectedTotal.doubleValue()));
    }

    @Test
    void rejectsMissingApiKey() throws Exception {
        mockMvc.perform(post("/vendor/commission-quote").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid api-key"));
    }

    @Test
    void rejectsWrongApiKey() throws Exception {
        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsSameLengthKeyDifferingOnlyInLastCharacter() throws Exception {
        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test-kez")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsKeyThatIsPrefixOfValidKey() throws Exception {
        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsServiceUnavailableWhenSimulatedFailureTriggers() throws Exception {
        when(errorSimulator.shouldFail()).thenReturn(true);

        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test-key")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Vendor service temporarily unavailable"));
    }

    @Test
    void returnsQuoteForValidRequest() throws Exception {
        when(errorSimulator.shouldFail()).thenReturn(false);

        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test-key")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").isNotEmpty())
                .andExpect(jsonPath("$.commissionRate").value(0.040))
                .andExpect(jsonPath("$.totalCommission").value(400.00));
    }

    @Test
    void rejectsHugeLoanAmountBeforeCalculating() throws Exception {
        mockMvc.perform(post("/vendor/commission-quote").header("api-key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loanAmount": 1e999999999, "loanTermInMonths": 36, "riskBand": "HIGH"}
                        """))
                .andExpect(status().isBadRequest());
    }
}
