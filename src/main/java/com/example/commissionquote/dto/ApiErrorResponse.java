package com.example.commissionquote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error returned for any failed request")
public record ApiErrorResponse(
        @Schema(description = "Human-readable error message")
        String message
) {
}
