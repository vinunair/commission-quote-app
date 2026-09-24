package com.example.commissionquote;

import com.example.commissionquote.service.VendorErrorSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Full path over real HTTP: /api/quotes -> VendorClient -> /vendor/commission-quote (api-key check) and back.
 * Only the random-failure decision is controlled; everything else is the production wiring and config.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class QuoteFlowIntegrationTest {

    private static final int PORT = findFreePort();

    private static final String REQUEST = """
            {"loanAmount": 10000, "loanTermInMonths": 36, "riskBand": "MEDIUM"}
            """;

    // Chosen before startup so ${server.port} (used by vendor.base-url) resolves to the real port,
    // exactly as in production. A random port (0) would leave the vendor URL pointing at port 0.
    @DynamicPropertySource
    static void serverPort(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
    }

    @MockitoBean
    private VendorErrorSimulator errorSimulator;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + PORT).build();
    }

    @Test
    void returnsQuoteFromVendorOverHttp() {
        when(errorSimulator.shouldFail()).thenReturn(false);

        client.post().uri("/api/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(REQUEST)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quoteId").isNotEmpty()
                .jsonPath("$.commissionRate").isEqualTo(0.025)
                .jsonPath("$.totalCommission").isEqualTo(250.00);
    }

    @Test
    void returnsBadGatewayWhenVendorFails() {
        when(errorSimulator.shouldFail()).thenReturn(true);

        client.post().uri("/api/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(REQUEST)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Unable to generate quote right now. Please try again.");
    }

    @Test
    void rejectsInvalidInputWithoutCallingVendor() {
        client.post().uri("/api/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"loanAmount": -1, "loanTermInMonths": 36, "riskBand": "LOW"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Loan amount must be greater than 0.");

        verifyNoInteractions(errorSimulator);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
