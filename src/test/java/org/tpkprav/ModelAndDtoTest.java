package org.tpkprav;

import org.junit.jupiter.api.Test;
import org.tpkprav.client.dto.StoreRequest;
import org.tpkprav.client.dto.StoreResponse;
import org.tpkprav.api.ApiError;
import org.tpkprav.api.ApiErrorDetail;
import org.tpkprav.api.ApiResponse;
import org.tpkprav.api.RequestContext;
import org.tpkprav.api.Status;
import org.tpkprav.dto.TokenRequest;
import org.tpkprav.dto.TokenResponse;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.MalformedTokenException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelAndDtoTest {

    // ── ApiResponse ──────────────────────────────────────────────────────────

    @Test
    void apiResponse_success_hasCorrectFields() {
        ApiResponse<String> r = ApiResponse.success("req-1", "hello");
        assertEquals("req-1", r.requestId());
        assertEquals(Status.SUCCESS, r.status());
        assertEquals("hello", r.data());
        assertNull(r.error());
        assertNotNull(r.timestamp());
    }

    @Test
    void apiResponse_error_hasCorrectFields() {
        ApiError err = ApiError.of("CODE_1", "Something went wrong");
        ApiResponse<Object> r = ApiResponse.error("req-2", err);
        assertEquals("req-2", r.requestId());
        assertEquals(Status.ERROR, r.status());
        assertNull(r.data());
        assertNotNull(r.error());
        assertEquals("CODE_1", r.error().code());
    }

    // ── ApiError ─────────────────────────────────────────────────────────────

    @Test
    void apiError_of_setsCodeAndMessage() {
        ApiError e = ApiError.of("ERR_001", "msg");
        assertEquals("ERR_001", e.code());
        assertEquals("msg", e.message());
        assertNull(e.details());
    }

    @Test
    void apiError_withDetails_setsAll() {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail("field1", "reason1"));
        ApiError e = new ApiError("ERR_002", "msg2", details);
        assertEquals(1, e.details().size());
        assertEquals("field1", e.details().get(0).field());
        assertEquals("reason1", e.details().get(0).reason());
    }

    // ── ApiErrorDetail ───────────────────────────────────────────────────────

    @Test
    void apiErrorDetail_recordAccessors() {
        ApiErrorDetail d = new ApiErrorDetail("uuid", "must not be blank");
        assertEquals("uuid", d.field());
        assertEquals("must not be blank", d.reason());
    }

    // ── Status enum ─────────────────────────────────────────────────────────

    @Test
    void status_valuesAreSuccessAndError() {
        assertEquals(2, Status.values().length);
        assertEquals(Status.SUCCESS, Status.valueOf("SUCCESS"));
        assertEquals(Status.ERROR, Status.valueOf("ERROR"));
    }

    // ── RequestContext ───────────────────────────────────────────────────────

    @Test
    void requestContext_getterSetter() {
        RequestContext ctx = new RequestContext();
        assertNull(ctx.getRequestId());
        ctx.setRequestId("abc");
        assertEquals("abc", ctx.getRequestId());
    }

    // ── TokenRequest ─────────────────────────────────────────────────────────

    @Test
    void tokenRequest_recordAccessors() {
        TokenRequest req = new TokenRequest("S1234567D", "uuid-val");
        assertEquals("S1234567D", req.nric());
        assertEquals("uuid-val", req.uuid());
    }

    // ── TokenResponse ────────────────────────────────────────────────────────

    @Test
    void tokenResponse_recordAccessors() {
        TokenResponse resp = new TokenResponse("tok", "Bearer", 900L);
        assertEquals("tok", resp.accessToken());
        assertEquals("Bearer", resp.tokenType());
        assertEquals(900L, resp.expiresIn());
    }

    // ── Exception hierarchy ──────────────────────────────────────────────────

    @Test
    void expiredTokenException_messageAccessible() {
        ExpiredTokenException e = new ExpiredTokenException("expired");
        assertEquals("expired", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void badSignatureException_messageOnly() {
        BadSignatureException e = new BadSignatureException("bad sig");
        assertEquals("bad sig", e.getMessage());
    }

    @Test
    void badSignatureException_messageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        BadSignatureException e = new BadSignatureException("bad sig", cause);
        assertEquals("bad sig", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void malformedTokenException_messageAndCause() {
        RuntimeException cause = new RuntimeException("parse error");
        MalformedTokenException e = new MalformedTokenException("malformed", cause);
        assertEquals("malformed", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void expiredTokenException_isInvalidTokenException() {
        assertTrue(new ExpiredTokenException("e") instanceof org.tpkprav.service.exception.InvalidTokenException);
    }

    @Test
    void badSignatureException_isInvalidTokenException() {
        assertTrue(new BadSignatureException("b") instanceof org.tpkprav.service.exception.InvalidTokenException);
    }

    @Test
    void malformedTokenException_isInvalidTokenException() {
        assertTrue(new MalformedTokenException("m", new RuntimeException()) instanceof org.tpkprav.service.exception.InvalidTokenException);
    }

    // ── StoreRequest ─────────────────────────────────────────────────────────

    @Test
    void storeRequest_recordAccessors() {
        StoreRequest req = new StoreRequest("S1234567D", "some-uuid");
        assertEquals("S1234567D", req.nric());
        assertEquals("some-uuid", req.uuid());
    }

    @Test
    void storeRequest_equality() {
        StoreRequest a = new StoreRequest("S1234567D", "u1");
        StoreRequest b = new StoreRequest("S1234567D", "u1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ── StoreResponse ─────────────────────────────────────────────────────────

    @Test
    void storeResponse_storedValues() {
        StoreResponse resp = new StoreResponse("stored", "Credential saved successfully");
        assertEquals("stored", resp.status());
        assertEquals("Credential saved successfully", resp.message());
    }

    @Test
    void storeResponse_recordAccessors() {
        StoreResponse resp = new StoreResponse("conflict", "UUID already registered");
        assertEquals("conflict", resp.status());
        assertEquals("UUID already registered", resp.message());
    }

    @Test
    void storeResponse_equality() {
        StoreResponse a = new StoreResponse("stored", "ok");
        StoreResponse b = new StoreResponse("stored", "ok");
        assertEquals(a, b);
    }

}