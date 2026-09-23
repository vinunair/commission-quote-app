package com.example.commissionquote.controller;

import com.example.commissionquote.config.VendorProperties;
import com.example.commissionquote.dto.LoanQuoteRequest;
import com.example.commissionquote.dto.QuoteResponse;
import com.example.commissionquote.dto.RiskBand;
import com.example.commissionquote.service.VendorErrorSimulator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Stands in for the external vendor's Commission Quote API, which is not yet available.
 * Mirrors the agreed contract: api-key auth and occasional random failures.
 */
@RestController
@RequestMapping("/vendor")
public class VendorMockController {

    private final VendorProperties vendorProperties;
    private final VendorErrorSimulator errorSimulator;

    public VendorMockController(VendorProperties vendorProperties, VendorErrorSimulator errorSimulator) {
        this.vendorProperties = vendorProperties;
        this.errorSimulator = errorSimulator;
    }

    @PostMapping("/commission-quote")
    public ResponseEntity<QuoteResponse> generateQuote(
            @RequestHeader(value = "api-key", required = false) String apiKey,
            @Valid @RequestBody LoanQuoteRequest request) {

        if (apiKey == null || !apiKey.equals(vendorProperties.apiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid api-key");
        }

        if (errorSimulator.shouldFail()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Vendor service temporarily unavailable");
        }

        BigDecimal commissionRate = commissionRateFor(request.riskBand());
        BigDecimal totalCommission = request.loanAmount()
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        QuoteResponse response = new QuoteResponse(UUID.randomUUID().toString(), commissionRate, totalCommission);
        return ResponseEntity.ok(response);
    }

    private BigDecimal commissionRateFor(RiskBand riskBand) {
        return switch (riskBand) {
            case LOW -> new BigDecimal("0.015");
            case MEDIUM -> new BigDecimal("0.025");
            case HIGH -> new BigDecimal("0.040");
        };
    }
}
