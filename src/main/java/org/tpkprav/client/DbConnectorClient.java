package org.tpkprav.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.tpkprav.client.dto.StoreRequest;
import org.tpkprav.client.dto.StoreResponse;

import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey = "db-connector")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DbConnectorClient {

    @POST
    @Path("/credentials")
    @Retry(
            maxRetries = 2,
            delay = 150,
            delayUnit = ChronoUnit.MILLIS,
            // Don't retry on server-side errors (4xx/5xx) or when the bulkhead/circuit is
            // already blocking — those aren't transient network failures
            abortOn = {WebApplicationException.class, BulkheadException.class, CircuitBreakerOpenException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 5,
            failureRatio = 0.5,
            delay = 10,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2,
            // Business errors (409 conflict, 400 bad request) are not circuit-breaking failures
            skipOn = WebApplicationException.class
    )
    @Bulkhead(value = 10, waitingTaskQueue = 20)
    @Fallback(
            value = DbConnectorFallback.class,
            // Let business errors (409 conflict) propagate directly to the controller
            skipOn = WebApplicationException.class
    )
    // request is the JSON body (unannotated = body in JAX-RS / MicroProfile REST Client)
    StoreResponse store(StoreRequest request, @HeaderParam("X-Request-Id") String requestId);
}