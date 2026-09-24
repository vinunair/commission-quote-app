# Commission Quote App

A small web app where a staff member enters loan details (amount, term, risk band), clicks **Generate Quote**, and sees a commission quote from a (mocked) external vendor API.

## Prerequisites

- JDK 21 (e.g. `brew install openjdk@21`)
- Nothing else. Gradle runs through the included wrapper, and the frontend is plain HTML/JS with no build step.

## Running

```bash
./gradlew bootRun
```

Then open <http://localhost:8080>.

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

One Spring Boot app on port 8080 exposes two APIs:

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
| `client/VendorClient` | Calls the vendor, adds the `api-key` header, and turns any failure into `VendorUnavailableException` |
| `service/VendorErrorSimulator` | Decides whether a vendor call fails at random. Kept separate so tests can control it |
| `exception/GlobalExceptionHandler` | Maps errors to consistent JSON `{ "message": ... }` responses |
| `config/VendorProperties` | Vendor settings from `application.yml` |
| `resources/static/` | `index.html`, `app.js`, `style.css` |

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
- **Commission rates are fixed per risk band in the mock** (LOW 1.5%, MEDIUM 2.5%, HIGH 4.0%). `totalCommission = loanAmount × rate`, rounded to cents. The real vendor will own this logic.
- **Money uses `BigDecimal`** to avoid floating-point rounding errors.
- **Frontend has no framework.** One form doesn't need React or a build pipeline.

## Edge cases handled

| Scenario | Behaviour |
|---|---|
| Missing field, `loanAmount <= 0`, term outside 1–480 months | `400` with a field-level message shown in the UI. The vendor is never called |
| `loanAmount` over 100,000,000 or more than 2 decimal places (including huge scientific-notation values like `1e999999999`) | `400`, rejected before any calculation, so an 11-character input can't expand into a billion-digit number |
| Unknown risk band or malformed JSON | `400` "Malformed request body or invalid field value" |
| Vendor returns an error (simulated 503, 401, etc.) | `502` "Unable to generate quote right now. Please try again." |
| Vendor times out or can't be reached | Same `502`, after the 3s timeout |
| Server can't be reached from the browser | UI shows a connection error message |
| Double submit | The button is disabled while a request is in flight |

## Tests

- `QuoteControllerTest`: happy path, vendor failure → 502, validation errors, unknown risk band
- `VendorMockControllerTest`: missing or wrong api-key → 401, forced failure → 503, correct commission calculation
- `VendorClientTest`: sends the api-key header, and turns error statuses and timeouts into `VendorUnavailableException`

## Possible improvements (out of scope for the timebox)

- Retry with backoff for temporary vendor failures
- Structured logging and a correlation ID on vendor calls
- Keep the real vendor API key out of `application.yml` (use an environment variable or secret store)
- Frontend tests

## AI Usage

<!-- TODO: fill in honestly before submitting. Suggested starting point: -->
I used Claude Code (Anthropic) as a coding assistant during this challenge. It helped me:

- Plan the architecture and project layout
- Generate the Spring Boot skeleton and write the backend classes, the static frontend (HTML/JS/CSS) and the tests
- Find a bug where the catch-all exception handler was turning the vendor mock's 401/503 responses into 500s
- Draft this README

I reviewed the generated code, ran the app and tests locally, and <describe what you changed/decided yourself>.
