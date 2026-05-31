package org.tpkprav;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tpkprav.api.ApiResponse;
import org.tpkprav.api.RequestContext;
import org.tpkprav.api.mapper.ExceptionMappers;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.MalformedTokenException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionMappersTest {

    private ExceptionMappers mappers;
    private RequestContext requestContext;

    @BeforeEach
    void setUp() throws Exception {
        mappers = new ExceptionMappers();
        requestContext = new RequestContext();
        requestContext.setRequestId("test-req-id");

        Field f = ExceptionMappers.class.getDeclaredField("requestContext");
        f.setAccessible(true);
        f.set(mappers, requestContext);
    }

    // ── mapInvalidToken ──────────────────────────────────────────────────────

    @Test
    void mapExpiredToken_returns401WithExpiredCode() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapInvalidToken(new ExpiredTokenException("expired"));
        assertEquals(401, resp.getStatus());
        assertEquals("AUTH_TOKEN_EXPIRED", resp.getEntity().error().code());
        assertEquals("test-req-id", resp.getEntity().requestId());
    }

    @Test
    void mapBadSignature_returns401WithBadSigCode() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapInvalidToken(new BadSignatureException("bad sig"));
        assertEquals(401, resp.getStatus());
        assertEquals("AUTH_BAD_SIGNATURE", resp.getEntity().error().code());
    }

    @Test
    void mapMalformedToken_returns401WithMalformedCode() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapInvalidToken(new MalformedTokenException("bad token", new RuntimeException()));
        assertEquals(401, resp.getStatus());
        assertEquals("AUTH_MALFORMED_TOKEN", resp.getEntity().error().code());
    }

    // ── mapValidation ────────────────────────────────────────────────────────

    @Test
    void mapValidation_returns400WithFieldDetails() {
        ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
        Path path = Mockito.mock(Path.class);
        Path.Node node = Mockito.mock(Path.Node.class);
        Mockito.when(node.getName()).thenReturn("nric");
        Mockito.when(path.iterator()).thenReturn(List.of(node).iterator());
        Mockito.when(violation.getPropertyPath()).thenReturn(path);
        Mockito.when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException cve = new ConstraintViolationException(Set.of(violation));
        RestResponse<ApiResponse<Object>> resp = mappers.mapValidation(cve);

        assertEquals(400, resp.getStatus());
        assertEquals("VALIDATION_001", resp.getEntity().error().code());
        assertEquals(1, resp.getEntity().error().details().size());
        assertEquals("nric", resp.getEntity().error().details().get(0).field());
    }

    // ── mapWebApplication ────────────────────────────────────────────────────

    @Test
    void mapWebApplication_404_returnsCorrectStatus() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapWebApplication(new NotFoundException("Not found"));
        assertEquals(404, resp.getStatus());
        assertEquals("HTTP_404", resp.getEntity().error().code());
    }

    @Test
    void mapWebApplication_400_returnsCorrectStatus() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapWebApplication(new WebApplicationException("bad request", 400));
        assertEquals(400, resp.getStatus());
        assertEquals("HTTP_400", resp.getEntity().error().code());
    }

    // ── mapUnexpected ────────────────────────────────────────────────────────

    @Test
    void mapUnexpected_returns500() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapUnexpected(new RuntimeException("boom"));
        assertEquals(500, resp.getStatus());
        assertEquals("INTERNAL_001", resp.getEntity().error().code());
    }

    @Test
    void mapUnexpected_withError_returns500() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapUnexpected(new AssertionError("assertion failed"));
        assertEquals(500, resp.getStatus());
    }

    @Test
    void mapWebApplication_409_returnsDuplicateUuidCode() {
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapWebApplication(new WebApplicationException(409));
        assertEquals(409, resp.getStatus());
        assertEquals("DUPLICATE_UUID_001", resp.getEntity().error().code());
        assertEquals("UUID already registered", resp.getEntity().error().message());
    }

    // ── requestId fallback when context is empty ─────────────────────────────

    @Test
    void mapUnexpected_withNoRequestId_generatesId() throws Exception {
        requestContext.setRequestId(null);
        RestResponse<ApiResponse<Object>> resp =
                mappers.mapUnexpected(new RuntimeException("boom"));
        assertEquals(500, resp.getStatus());
        assertNotNull(resp.getEntity().requestId());
    }
}