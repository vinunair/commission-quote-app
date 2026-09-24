# Commission Quote App

A small web app where a staff member enters loan details (amount, term, risk band), clicks **Generate Quote**, and sees a commission quote from a (mocked) external vendor API.

## Prerequisites

- JDK 21 (any distribution, e.g. [Eclipse Temurin](https://adoptium.net))
- Nothing else. Gradle runs through the included wrapper, and the frontend is plain HTML/JS with no build step.

## Running

```bash
./gradlew bootRun
```

Then open <http://localhost:8080>.

To use a different port, run `./gradlew bootRun --args='--server.port=9090'` (or set `SERVER_PORT`). The vendor mock URL follows the app's port automatically.

## API documentation (OpenAPI / Swagger)

With the app running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>. Browse the quotes endpoint and try it out.
- OpenAPI spec (JSON): <http://localhost:8080/v3/api-docs>

A copy of the spec is also saved as [`docs/openapi.yaml`](docs/openapi.yaml), so you can read it without running the app. If the API changes, re-export it with the app running:

```bash
curl -s http://localhost:8080/v3/api-docs.yaml -o docs/openapi.yaml
```

The spec is generated from the code by [springdoc-openapi](https://springdoc.org), so it stays in sync with the controller and validation rules. It covers only the app's API (`/api/**`). The vendor mock is left out on purpose.

## Running tests

```bash
./gradlew test
```

## Architecture

One Spring Boot app (port 8080 by default) exposes two APIs:

```
Browser (static HTML + fetch)
   │  POST /api/quotes            { loanAmount, loanTermInMonths, riskBand }
   ▼
QuoteController ── validates ──▶ VendorClient (RestClient, 3s timeouts)
                                    │  POST /vendor/commission-quote
                                    │  header  api-key: <configured key>
                                    ▼
                                 VendorMockController
                                   - 401 if api-key is missing or wrong
                                   - ~20% chance of a simulated 503
                                   - otherwise { quoteId, commissionRate, totalCommission }
```

| Path | Purpose |
|---|---|
| `controller/QuoteController` | The app's API used by the UI |
| `controller/VendorMockController` | Stand-in for the vendor API, which isn't available yet |
| `client/VendorClient` | Calls the vendor, adds the `api-key` header, and turns any failure (error status, timeout, empty or incomplete response) into `VendorUnavailableException` |
| `service/VendorErrorSimulator` | Decides whether a vendor call fails at random. Kept separate so tests can control it |
| `exception/GlobalExceptionHandler` | Maps errors to consistent JSON `{ "message": ... }` responses. Spring's own web errors (404, 405, 415) keep their status; only unexpected errors become a logged 500 |
| `config/VendorProperties` | Vendor settings from `application.yml` |
| `config/OpenApiConfig` | Title and description for the generated OpenAPI spec |
| `resources/static/` | `index.html`, `app.js`, `style.css` |
| `docs/openapi.yaml` | Exported copy of the OpenAPI spec |

### Configuration (`application.yml`)

| Property | Default | Meaning |
|---|---|---|
| `vendor.api-key` | `local-dev-secret` | Key the client sends and the mock checks |
| `vendor.base-url` | `http://localhost:${server.port:8080}` | Where the vendor API lives. Defaults to this app's own port (the mock), so changing `server.port` or `SERVER_PORT` still works. Point it at the real vendor later |
| `vendor.error-rate-percent` | `20` | Chance (%) that the mock returns a failure |
| `vendor.connect-timeout-ms` / `read-timeout-ms` | `3000` | Client timeouts for vendor calls |

Any property can be overridden at launch, e.g. `./gradlew bootRun --args='--vendor.error-rate-percent=0'`.

## Design decisions

- **The vendor mock runs in the same app but is called over real HTTP.** One process keeps the app easy to run, and the real HTTP call means the `api-key` header, error statuses and timeouts behave as they would against the real vendor. Switching to the real vendor only means changing `vendor.base-url` and `vendor.api-key`.
- **The mock's commission rate depends on risk band and loan term.** Base rate by risk band: LOW 1.5%, MEDIUM 2.5%, HIGH 4.0%. Then a term adjustment: +0.5% for terms under 12 months, none for 12–60 months, −0.5% for terms over 60 months (e.g. HIGH over 36 months is 4.0%, over 61 months 3.5%). Shorter loans pay a higher rate because the lender earns interest for less time, so there is less ongoing value to share. `totalCommission = loanAmount × rate`, rounded to cents. The brief doesn't define pricing, so this is an illustrative rule; the real vendor will own this logic.
- **Money uses `BigDecimal`** to avoid floating-point rounding errors.
- **Frontend has no framework.** One form doesn't need React or a build pipeline.

## Edge cases handled

| Scenario | Behaviour |
|---|---|
| Missing field, `loanAmount <= 0`, term outside 1–480 months | `400` with a plain-English message shown in the UI (e.g. "Loan amount must be greater than 0."). The vendor is never called |
| `loanAmount` over 100,000,000, or more than 9 whole digits or 2 decimal places (including huge scientific-notation values like `1e999999999`) | `400`, rejected before any calculation, so an 11-character input can't expand into a billion-digit number |
| Unknown risk band or malformed JSON | `400` "The loan details could not be read. Please check them and try again." |
| Vendor returns an error (simulated 503, 401, etc.) | `502` "Unable to generate quote right now. Please try again." |
| Vendor times out or can't be reached | Same `502`, after the 3s timeout |
| Vendor returns success with an empty or incomplete body | Same `502`, and the cause is logged. Without this, the UI would show a blank quote with 0% and $0.00 |
| Wrong HTTP method, non-JSON body, or unknown path | `405`, `415` or `404` with a message, not a `500` |
| Server can't be reached from the browser | UI shows a connection error message |
| Double submit | The button is disabled while a request is in flight |

## Tests

32 tests in total:

- `QuoteControllerTest` (10): happy path, vendor failure → 502, validation errors, unknown risk band, oversized `loanAmount` (`1e999999999`) and too many decimal places → 400, wrong method → 405, non-JSON body → 415, unknown path → 404
- `VendorMockControllerTest` (13): missing, wrong, same-length-but-wrong or prefix-only api-key → 401 (checked in constant time), forced failure → 503, correct commission calculation, term adjustment at each tier boundary (1, 11, 12, 60, 61 and 480 months, as one parameterized test), oversized `loanAmount` rejected before any calculation
- `VendorClientTest` (5): sends the api-key header; turns error statuses, timeouts, empty bodies and incomplete bodies into `VendorUnavailableException`
- `QuoteFlowIntegrationTest` (3): end to end over real HTTP, from `/api/quotes` through `VendorClient` to the vendor mock with its api-key check. Covers a successful quote, a vendor failure → 502, and invalid input → 400 without the vendor being called. Only the random-failure decision is controlled; the rest is the production wiring and config
- `CommissionQuoteAppApplicationTests` (1): the application context starts

## Possible improvements (out of scope for the timebox)

- Retry with backoff for temporary vendor failures
- Structured logging and a correlation ID on vendor calls
- Keep the real vendor API key out of `application.yml` (use an environment variable or secret store)
- Frontend tests
- Enable the vendor mock and Swagger UI only outside production (e.g. `@Profile("dev")`)
- API versioning
- circuit breaker and bulkhead config
- Switch vendors
- Rate limiting



## AI Usage

I used **Claude Code** (Anthropic) as a coding assistant, with me directing the work and making the decisions.

### Design and direction
- Set the requirements and chose the architecture: Spring Boot and Java (my strongest stack), one app with the vendor mock called over real HTTP so the `api-key` check and timeouts behave like a real integration, and a plain HTML/JS frontend with no framework.
- Had Claude produce an implementation plan to my constraints, then generate the code, tests and OpenAPI docs from it.

### Review and hardening
I treated the generated code as a first draft and put it through several review passes:
- **Ran the app and tests locally** and checked success, validation and vendor-failure cases. [check]
- **Questioned the design** until I was confident in each choice and its trade-offs, e.g. POST vs GET for quotes, `ThreadLocalRandom` for the failure simulation, and keeping the vendor mock in the same app.
- **Ran automated code and security reviews** with Claude Code, judged each finding, and directed the fixes, each backed by tests:
  - Spring's own 404/405/415 errors were being returned as 500s by the catch-all handler.
  - `loanAmount` had no size limit, so `1e999999999` could tie up CPU and memory.
  - Empty or incomplete vendor responses reached the UI as a blank $0.00 quote.
  - The vendor URL was hardcoded to port 8080, so changing the port broke every quote.
  - The api-key comparison wasn't constant-time.
- **Reviewed the result against the brief** and closed the gaps I found: plain-English validation messages for users, and an end-to-end integration test covering the full path over real HTTP.
- **Chose what not to fix** within the timebox, and documented it under *Possible improvements*.

I understand the code in this repository and can walk through any part of it and the reasoning behind it.



