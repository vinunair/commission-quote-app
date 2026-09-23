package com.example.commissionquote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Commission quote returned by the vendor")
public record QuoteResponse(
        @Schema(description = "Vendor-issued quote identifier", example = "dbec8c56-07f9-4d21-946e-ca603d81b6f8")
        String quoteId,

        @Schema(description = "Commission rate as a fraction (0.025 = 2.5%)", example = "0.025")
        BigDecimal commissionRate,

        @Schema(description = "Total commission in dollars", example = "250.00")
        BigDecimal totalCommission
) {
}
