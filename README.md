# tpk-fapi-api

A Financial-grade API (FAPI) token service built with Quarkus. Issues signed JWT Bearer tokens after validating identity fields, with production-grade fault tolerance, async logging, and request correlation built in.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 (compiled), JVM 21+ required |
| Framework | [Quarkus 3.36](https://quarkus.io/) |
| REST | Quarkus REST (RESTEasy Reactive) |
| Concurrency | Project Loom — `@RunOnVirtualThread` on all endpoints |
| Fault Tolerance | SmallRye Fault Tolerance — `@Retry`, `@CircuitBreaker`, `@Bulkhead`, `@Fallback` |
| Validation | Hibernate Validator (Jakarta Bean Validation) |
| JWT | [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt) — HS256 signing & verification |
| Logging | SLF4J + JBoss LogManager + custom `AsyncLogger` (virtual-thread backed) |
| Build | Maven Wrapper (`mvnw`) |

---

## Project Structure

```
src/
├── main/java/org/tpkprav/
│   ├── GreetingResource.java          # GET /api/hello — health/smoke endpoint
│   ├── api/
│   │   ├── ApiResponse.java           # Unified response envelope
│   │   ├── ApiError.java              # Error payload
│   │   ├── ApiErrorDetail.java        # Per-field validation detail
│   │   ├── RequestContext.java        # Request-scoped X-Request-Id holder
│   │   ├── Status.java                # SUCCESS / ERROR enum
│   │   ├── filter/
│   │   │   └── ApiResponseFilters.java  # Request/response filter: correlation ID, response wrapping
│   │   └── mapper/
│   │       └── ExceptionMappers.java    # Global exception → ApiResponse mappers
│   ├── controller/
│   │   ├── AuthController.java        # POST /api/v1/auth/token
│   │   └── TokenFallbackHandler.java  # Fault tolerance fallback for token endpoint
│   ├── dto/
│   │   ├── TokenRequest.java          # { nric, uuid }
│   │   └── TokenResponse.java        # { accessToken, tokenType, expiresIn }
│   ├── logging/
│   │   └── AsyncLogger.java          # Non-blocking logger backed by a virtual-thread executor
│   ├── service/
│   │   ├── JwtService.java            # Interface: createToken / verifyToken
│   │   ├── impl/
│   │   │   └── JwtServiceImpl.java   # HS256 JWT implementation via Nimbus
│   │   └── exception/
│   │       ├── InvalidTokenException.java
│   │       ├── ExpiredTokenException.java
│   │       ├── BadSignatureException.java
│   │       └── MalformedTokenException.java
│   └── util/
│       └── MaskingUtils.java         # PII masking (NRIC last-4 visible)
│
└── test/java/org/tpkprav/
    ├── AuthApiKarateTest.java         # Karate BDD runner (@QuarkusTest)
    ├── AuthControllerTest.java        # REST Assured integration tests
    ├── ExceptionMappersTest.java      # Exception mapper tests
    ├── FaultToleranceTest.java        # Circuit breaker / retry tests
    ├── GreetingResourceTest.java      # Greeting endpoint tests
    ├── JwtServiceImplTest.java        # JWT unit tests
    ├── AsyncLoggerTest.java           # Async logger unit tests
    ├── ModelAndDtoTest.java           # DTO / model tests
    ├── KarateTestProfile.java         # Quarkus test profile (port 8083)
    └── util/
        └── MaskingUtilsTest.java      # NRIC masking unit tests
```

---

## API Endpoints

All endpoints are served under the root path `/api` (configured via `quarkus.http.root-path`).

### `GET /api/hello`

Smoke/health endpoint.

**Response** `200 text/plain`
```
Hello from Quarkus REST
```

---

### `POST /api/v1/auth/token`

Issues a signed HS256 JWT Bearer token.

**Request** `application/json`
```json
{
  "nric": "S1234567D",
  "uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Constraint |
|---|---|
| `nric` | Required, not blank, min 4 characters |
| `uuid` | Required, not blank |

**Response** `200 application/json`
```json
{
  "status": "SUCCESS",
  "requestId": "be1e9352-f496-495c-9b35-092e83de71b2",
  "timestamp": "2026-05-31T01:30:32.754Z",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Error responses**

| Status | Code | Trigger |
|---|---|---|
| `400` | `VALIDATION_001` | Missing or blank fields |
| `503` | — | Circuit open or retries exhausted (fallback fires) |

**Headers**

Every response includes `X-Request-Id`. If the caller supplies it on the request it is echoed back verbatim; otherwise a UUID is generated.

---

## Fault Tolerance

The token endpoint is protected by a full SmallRye Fault Tolerance stack:

| Annotation | Config |
|---|---|
| `@Retry` | 2 retries, 100 ms delay, only on `IllegalStateException` |
| `@CircuitBreaker` | Opens after 50 % failures in a 10-request window; recovers after 5 s with 2 consecutive successes |
| `@Bulkhead` | 20 concurrent calls max, queue of 50 waiting |
| `@Fallback` | `TokenFallbackHandler` — logs a warning and throws `503 ServiceUnavailableException`; skips on validation errors |

---

## Security

- **JWT signing**: HS256 via Nimbus JOSE+JWT. Secret is Base64-encoded and must decode to ≥ 32 bytes.
- **Secret injection**: Set the `JWT_SECRET` environment variable in any non-dev environment. Falls back to a hardcoded dev key if unset.
- **PII masking**: NRIC is masked in all log output (`*****567D`) via `MaskingUtils.maskNric()`. The raw value never appears in logs.
- **Request correlation**: `X-Request-Id` header is propagated through every log line via MDC.

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `quarkus.http.port` | `8080` | HTTP listen port |
| `quarkus.http.root-path` | `/api` | Global URL prefix for all endpoints |
| `jwt.secret` | dev key | Base64-encoded HMAC secret — **override via `JWT_SECRET` env var** |
| `jwt.issuer` | `tpk-fapi-api` | JWT `iss` claim |
| `jwt.expiration-minutes` | `15` | Token TTL (seconds = value × 60) |

---

## Running the Application

**Prerequisites:** Java 21+, Maven (or use the included `mvnw`).

```bash
# Development mode (live reload)
./mvnw quarkus:dev

# Run packaged JAR
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Override JWT secret at runtime
JWT_SECRET=<base64-secret> java -jar target/quarkus-app/quarkus-run.jar
```

The server starts on `http://localhost:8080`. All endpoints are reachable at `http://localhost:8080/api/...`.

---

## Testing

### Frameworks

| Framework | Purpose |
|---|---|
| [JUnit 5](https://junit.org/junit5/) | Test runner for all test classes |
| [Quarkus JUnit](https://quarkus.io/guides/getting-started-testing) | `@QuarkusTest` — starts a real Quarkus server in-process |
| [REST Assured](https://rest-assured.io/) | HTTP integration assertions |
| [Mockito](https://site.mockito.org/) | `@InjectMock` / `@MockBean` for unit-level isolation |
| [Karate 1.5.1](https://karatelabs.github.io/karate/) | BDD API tests via `.feature` files |
| [JaCoCo](https://www.jacoco.org/) | Code coverage reporting |
| [PITest 1.20](https://pitest.org/) + [JUnit 5 plugin](https://github.com/hcoles/pitest) | Mutation testing for unit and service layer |

### Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=AuthApiKarateTest

# Tests + JaCoCo coverage report (output: target/site/jacoco/index.html)
./mvnw verify

# Mutation testing only (output: target/pit-reports/index.html)
./mvnw pitest:mutationCoverage
```

### Test Profiles

`KarateTestProfile` starts Quarkus on port `8083` for Karate tests, isolating them from the main test suite which runs on port `8081`.

### Karate Features

| Feature file | Scenarios |
|---|---|
| `karate/auth.feature` | 14 — full positive + negative coverage of the token endpoint |
| `karate/greeting.feature` | 1 — smoke test for the greeting endpoint |

Karate reports are generated at `target/karate-reports/karate-summary.html` after each run.

---

## Mutation Testing

Mutation testing is configured with [PITest](https://pitest.org/) and the JUnit 5 plugin. It verifies that the unit tests are strong enough to detect real code defects, not just achieve line coverage.

### Scope

Only classes testable by pure JUnit 5 unit tests are included. `@QuarkusTest` classes (which start a full Quarkus container) are excluded because PITest cannot drive Quarkus' classloader.

| Layer | Mutated classes | Test classes used |
|---|---|---|
| Util | `MaskingUtils` | `MaskingUtilsTest` |
| API model | `ApiResponse`, `ApiError`, `ApiErrorDetail`, `Status`, `RequestContext` | `ModelAndDtoTest` |
| API mapper | `ExceptionMappers` | `ExceptionMappersTest` |
| DTO | `TokenRequest`, `TokenResponse` | `ModelAndDtoTest` |
| Service exceptions | `InvalidTokenException`, `ExpiredTokenException`, `BadSignatureException`, `MalformedTokenException` | `ModelAndDtoTest`, `ExceptionMappersTest` |

### Thresholds

| Metric | Threshold | Current result |
|---|---|---|
| Mutation score | ≥ 80 % | **95 %** (19 / 20 mutations killed) |
| Line coverage | ≥ 80 % | **100 %** |

The build fails automatically if either threshold is not met.

### Configuration notes

- `avoidCallsTo` is set for `AsyncLogger` and `RequestContext` — mutations on logging calls and request-context setters are structurally undetectable without a dedicated log-capture harness and are not considered meaningful business-logic mutations.
- `timestampedReports=false` so the HTML report always lands at `target/pit-reports/index.html` (no timestamped sub-folder).
- Runs with 2 parallel threads for faster execution.

### Viewing the report

```bash
./mvnw pitest:mutationCoverage
open target/pit-reports/index.html
```

---

## Design Notes

- **Virtual threads everywhere**: Both `AuthController` and `GreetingResource` are annotated with `@RunOnVirtualThread`. Requests are dispatched to lightweight JVM virtual threads (Project Loom), so blocking I/O never ties up a platform thread.
- **Async logging**: `AsyncLogger` offloads all log writes to a dedicated virtual-thread-backed executor. The request thread is never blocked by log I/O. MDC (`requestId`) is captured at dispatch time and restored on the logging thread.
- **Unified response envelope**: `ApiResponseFilters` wraps every JSON response in `{ status, requestId, timestamp, data }` transparently, so controllers return plain DTOs.
- **Structured exception handling**: `ExceptionMappers` maps validation errors, JWT errors, and unexpected exceptions to consistent `ApiResponse` payloads with typed error codes.