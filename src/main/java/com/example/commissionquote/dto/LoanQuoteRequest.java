package com.example.commissionquote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Loan details to quote a commission for")
public record LoanQuoteRequest(

        @Schema(description = "Loan amount in dollars", example = "10000.00")
        @NotNull(message = "loanAmount is required")
        @DecimalMin(value = "0.01", message = "loanAmount must be greater than 0")
        BigDecimal loanAmount,

        @Schema(description = "Loan term in months", example = "36")
        @NotNull(message = "loanTermInMonths is required")
        @Min(value = 1, message = "loanTermInMonths must be at least 1")
        @Max(value = 480, message = "loanTermInMonths must be at most 480")
        Integer loanTermInMonths,

        @Schema(description = "Risk band of the borrower", example = "MEDIUM")
        @NotNull(message = "riskBand is required")
        RiskBand riskBand
) {
}
