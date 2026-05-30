package org.tpkprav.api.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.tpkprav.api.ApiResponse;
import org.slf4j.MDC;
import org.tpkprav.api.RequestContext;
import org.tpkprav.logging.AsyncLogger;

import java.util.UUID;

@ApplicationScoped
public class ApiResponseFilters {

    private static final AsyncLogger log = AsyncLogger.of(ApiResponseFilters.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Inject
    RequestContext requestContext;

    @ServerRequestFilter(preMatching = true)
    public void onRequest(ContainerRequestContext request) {
        String requestId = request.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        requestContext.setRequestId(requestId);
        MDC.put(MDC_REQUEST_ID, requestId);
        log.debug("--> {} {}", request.getMethod(), request.getUriInfo().getRequestUri());
    }

    @ServerResponseFilter
    public void onResponse(ContainerRequestContext request, ContainerResponseContext response) {
        String requestId = requestContext.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            requestContext.setRequestId(requestId);
            MDC.put(MDC_REQUEST_ID, requestId);
        }
        response.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);

        try {
            Object entity = response.getEntity();
            if (entity instanceof ApiResponse<?>) {
                return;
            }
            MediaType mediaType = response.getMediaType();
            if (mediaType != null && !MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
                return;
            }
            if (entity == null) {
                return;
            }
            response.setEntity(ApiResponse.success(requestId, entity));
        } finally {
            log.debug("<-- {} {} {}", request.getMethod(), request.getUriInfo().getRequestUri(), response.getStatus());
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
