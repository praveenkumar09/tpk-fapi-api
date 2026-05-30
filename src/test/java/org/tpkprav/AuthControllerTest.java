package org.tpkprav;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthControllerTest {

    private static final String TOKEN_PATH = "/v1/auth/token";
    private static final String VALID_BODY =
            "{\"nric\":\"S1234567D\",\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\"}";

    // ── Positive ─────────────────────────────────────────────────────────────

    @Test
    void validRequest_returns200WithToken() {
        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .body("data.accessToken", notNullValue())
            .body("data.tokenType", equalTo("Bearer"))
            .body("data.expiresIn", equalTo(900))
            .body("requestId", notNullValue())
            .body("timestamp", notNullValue());
    }

    @Test
    void customRequestId_isEchoedInResponseHeaderAndBody() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Request-Id", "trace-12345")
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(200)
            .header("X-Request-Id", equalTo("trace-12345"))
            .body("requestId", equalTo("trace-12345"));
    }

    @Test
    void noRequestId_generatesUuid() {
        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(200)
            .header("X-Request-Id", matchesPattern(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    // ── Validation (negative) ─────────────────────────────────────────────────

    @Test
    void missingNric_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\"}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("status", equalTo("ERROR"))
            .body("error.code", equalTo("VALIDATION_001"))
            .body("error.details", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    void missingUuid_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"nric\":\"S1234567D\"}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("status", equalTo("ERROR"))
            .body("error.code", equalTo("VALIDATION_001"));
    }

    @Test
    void blankNric_returns400WithFieldDetail() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"nric\":\"\",\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\"}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("error.details.field", hasItem("nric"));
    }

    @Test
    void blankUuid_returns400WithFieldDetail() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"nric\":\"S1234567D\",\"uuid\":\"\"}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("error.details.field", hasItem("uuid"));
    }

    @Test
    void whitespaceOnlyFields_returns400WithTwoErrors() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"nric\":\"   \",\"uuid\":\"   \"}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("error.details", hasSize(2));
    }

    @Test
    void emptyBody_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post(TOKEN_PATH)
        .then()
            .statusCode(400)
            .body("status", equalTo("ERROR"));
    }

    // ── Method not allowed (negative) ─────────────────────────────────────────

    @Test
    void getOnTokenEndpoint_returns405() {
        given()
        .when()
            .get(TOKEN_PATH)
        .then()
            .statusCode(405);
    }

    @Test
    void putOnTokenEndpoint_returns405() {
        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .put(TOKEN_PATH)
        .then()
            .statusCode(405);
    }

    @Test
    void deleteOnTokenEndpoint_returns405() {
        given()
        .when()
            .delete(TOKEN_PATH)
        .then()
            .statusCode(405);
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void nonExistentPath_returns404() {
        given()
            .contentType(ContentType.JSON)
            .body(VALID_BODY)
        .when()
            .post("/v1/auth/nonexistent")
        .then()
            .statusCode(404);
    }
}