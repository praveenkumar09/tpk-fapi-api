package org.tpkprav.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServiceUnavailableException;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.tpkprav.api.RequestContext;
import org.tpkprav.client.dto.StoreResponse;
import org.tpkprav.logging.AsyncLogger;

@ApplicationScoped
public class DbConnectorFallback implements FallbackHandler<StoreResponse> {

    private static final AsyncLogger log = AsyncLogger.of(DbConnectorFallback.class);

    @Inject
    RequestContext requestContext;

    @Override
    public StoreResponse handle(ExecutionContext context) {
        log.warn("requestId={} db-connector unavailable (circuit open or retries exhausted) cause={}",
                requestContext.getRequestId(),
                context.getFailure().getClass().getSimpleName());
        throw new ServiceUnavailableException("Credential store is temporarily unavailable, please retry later");
    }
}