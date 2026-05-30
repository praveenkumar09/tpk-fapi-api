package org.tpkprav.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServiceUnavailableException;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.tpkprav.api.RequestContext;
import org.tpkprav.dto.TokenRequest;
import org.tpkprav.dto.TokenResponse;
import org.tpkprav.logging.AsyncLogger;

@ApplicationScoped
public class TokenFallbackHandler implements FallbackHandler<TokenResponse> {

    private static final AsyncLogger log = AsyncLogger.of(TokenFallbackHandler.class);

    @Inject
    RequestContext requestContext;

    @Override
    public TokenResponse handle(ExecutionContext context) {
        String maskedNric = "****";
        if (context.getParameters().length > 0
                && context.getParameters()[0] instanceof TokenRequest req) {
            maskedNric = AuthController.maskNric(req.nric());
        }
        log.warn("requestId={} Token service unavailable (circuit open or retries exhausted) nric={}",
                requestContext.getRequestId(), maskedNric);
        throw new ServiceUnavailableException("Token service temporarily unavailable, please retry later");
    }
}