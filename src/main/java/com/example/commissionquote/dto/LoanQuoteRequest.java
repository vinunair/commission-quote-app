package com.example.commissionquote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Loan details to quote a commission for")
public record LoanQuoteRequest(

        @Schema(description = "Loan amount in dollars", example = "10000.00")
        @NotNull(message = "Loan amount is required")
        @DecimalMin(value = "0.01", message = "Loan amount must be greater than 0")
        // Caps size as well as value: without these, e.g. 1e999999999 passes validation and
        // later expands into a billion-digit number when the commission is rounded.
        @DecimalMax(value = "100000000.00", message = "Loan amount must be at most 100,000,000")
        @Digits(integer = 9, fraction = 2, message = "Loan amount can have at most 9 whole digits and 2 decimal places")
        BigDecimal loanAmount,

        @Schema(description = "Loan term in months", example = "36")
        @NotNull(message = "Loan term is required")
        @Min(value = 1, message = "Loan term must be at least 1 month")
        @Max(value = 480, message = "Loan term must be at most 480 months")
        Integer loanTermInMonths,

        @Schema(description = "Risk band of the borrower", example = "MEDIUM")
        @NotNull(message = "Risk band is required")
        RiskBand riskBand
) {
}
