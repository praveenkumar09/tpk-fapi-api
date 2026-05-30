package org.tpkprav.api.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.tpkprav.api.ApiResponse;
import org.tpkprav.api.RequestContext;

import java.util.UUID;

@ApplicationScoped
public class ApiResponseFilters {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Inject
    RequestContext requestContext;

    @ServerRequestFilter(preMatching = true)
    public void onRequest(ContainerRequestContext request) {
        String requestId = request.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        requestContext.setRequestId(requestId);
    }

    @ServerResponseFilter
    public void onResponse(ContainerResponseContext response) {
        String requestId = requestContext.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            requestContext.setRequestId(requestId);
        }
        response.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);

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
    }
}
