package org.tpkprav;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tpkprav.service.JwtService;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.MalformedTokenException;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests fault-tolerance behaviour: retry, fallback handler, and the paths
 * where a mocked JwtService throws different exceptions.
 *
 * The FallbackHandler catches ALL exceptions (no skipOn), so every
 * failure from createToken returns 503 via the handler.
 * The 401 paths (mapInvalidToken) are covered by ExceptionMappersTest.
 */
@QuarkusTest
class FaultToleranceTest {

    @InjectMock
    JwtService jwtService;

    private static final String TOKEN_PATH = "/v1/auth/token";
    private static final String VALID_BODY =
            "{\"nric\":\"S1234567D\",\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\"}";

    // ── Retry + Fallback ─────────────────────────────────────────────────────

    @Test
    void illegalStateFromService_retriesAndFallsBackTo503() {
        Mockito.when(jwtService.createToken(any()))
               .thenThrow(new IllegalStateException("JWT signing failed"));

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(503);

        // maxRetries=1 in test profile → 2 total calls (initial + 1 retry)
        Mockito.verify(jwtService, Mockito.times(2)).createToken(any());
    }

    @Test
    void runtimeExceptionFromService_noRetry_fallbackInvoked_returns503() {
        // RuntimeException is NOT in retryOn (only IllegalStateException is), so no retry
        Mockito.when(jwtService.createToken(any()))
               .thenThrow(new RuntimeException("unexpected failure"));

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(503);

        Mockito.verify(jwtService, Mockito.times(1)).createToken(any());
    }

    // ── InvalidTokenExceptions: fallback handler catches them → 503 ──────────

    @Test
    void expiredTokenFromService_fallbackReturns503() {
        Mockito.when(jwtService.createToken(any()))
               .thenThrow(new ExpiredTokenException("Token is expired"));

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(503);
    }

    @Test
    void badSignatureFromService_fallbackReturns503() {
        Mockito.when(jwtService.createToken(any()))
               .thenThrow(new BadSignatureException("Bad signature"));

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(503);
    }

    @Test
    void malformedTokenFromService_fallbackReturns503() {
        Mockito.when(jwtService.createToken(any()))
               .thenThrow(new MalformedTokenException("Malformed", new RuntimeException()));

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(503);
    }

    // ── Success path (no fault tolerance triggered) ───────────────────────────

    @Test
    void successFromService_returns200() {
        Mockito.when(jwtService.createToken(any()))
               .thenReturn("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.fake");

        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(200);
    }
}