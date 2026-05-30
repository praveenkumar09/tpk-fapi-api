package org.tpkprav.api.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tpkprav.api.ApiError;
import org.tpkprav.api.ApiErrorDetail;
import org.tpkprav.api.ApiResponse;
import org.tpkprav.api.RequestContext;
import org.tpkprav.logging.AsyncLogger;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.InvalidTokenException;
import org.tpkprav.service.exception.MalformedTokenException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExceptionMappers {

    private static final AsyncLogger log = AsyncLogger.of(ExceptionMappers.class);

    @Inject
    RequestContext requestContext;

    @ServerExceptionMapper
    public RestResponse<ApiResponse<Object>> mapInvalidToken(InvalidTokenException e) {
        String code = switch (e) {
            case ExpiredTokenException ignored -> "AUTH_TOKEN_EXPIRED";
            case BadSignatureException ignored -> "AUTH_BAD_SIGNATURE";
            case MalformedTokenException ignored -> "AUTH_MALFORMED_TOKEN";
        };
        log.warn("Token rejected code={} message={}", code, e.getMessage());
        return RestResponse.status(
                Response.Status.UNAUTHORIZED,
                ApiResponse.error(currentRequestId(), ApiError.of(code, e.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<ApiResponse<Object>> mapValidation(ConstraintViolationException e) {
        List<ApiErrorDetail> details = e.getConstraintViolations().stream()
                .map(v -> new ApiErrorDetail(lastNode(v.getPropertyPath()), v.getMessage()))
                .toList();
        log.warn("Validation failed violations={}", details.size());
        return RestResponse.status(
                Response.Status.BAD_REQUEST,
                ApiResponse.error(
                        currentRequestId(),
                        new ApiError("VALIDATION_001", "Validation failed", details)));
    }

    @ServerExceptionMapper
    public RestResponse<ApiResponse<Object>> mapWebApplication(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        log.warn("HTTP {} {}", status, e.getMessage());
        return RestResponse.status(
                Response.Status.fromStatusCode(status),
                ApiResponse.error(
                        currentRequestId(),
                        ApiError.of("HTTP_" + status, e.getMessage())));
    }

    @ServerExceptionMapper
    public RestResponse<ApiResponse<Object>> mapUnexpected(Throwable e) {
        log.error("Unhandled exception", e);
        return RestResponse.status(
                Response.Status.INTERNAL_SERVER_ERROR,
                ApiResponse.error(
                        currentRequestId(),
                        ApiError.of("INTERNAL_001", "Internal server error")));
    }

    private String currentRequestId() {
        String id = requestContext.getRequestId();
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            requestContext.setRequestId(id);
        }
        return id;
    }

    private static String lastNode(Path path) {
        String last = null;
        for (Path.Node node : path) {
            last = node.getName();
        }
        return last;
    }
}
