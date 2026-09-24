package com.example.commissionquote.controller;

import com.example.commissionquote.client.VendorClient;
import com.example.commissionquote.dto.ApiErrorResponse;
import com.example.commissionquote.dto.LoanQuoteRequest;
import com.example.commissionquote.dto.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Quotes", description = "Commission quote generation")
public class QuoteController {

    private final VendorClient vendorClient;

    public QuoteController(VendorClient vendorClient) {
        this.vendorClient = vendorClient;
    }

    @PostMapping(value = "/quotes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a commission quote",
            description = "Validates the loan details and requests a commission quote from the vendor.")
    @ApiResponse(responseCode = "200", description = "Quote generated")
    @ApiResponse(responseCode = "400", description = "Invalid loan details or malformed request body",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "Loan amount must be greater than 0."}""")))
    @ApiResponse(responseCode = "502", description = "Vendor failed, timed out or was unreachable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {"message": "Unable to generate quote right now. Please try again."}""")))
    public QuoteResponse generateQuote(@Valid @RequestBody LoanQuoteRequest request) {
        return vendorClient.requestQuote(request);
    }
}
