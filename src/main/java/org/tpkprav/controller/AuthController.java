package org.tpkprav.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tpkprav.api.RequestContext;
import org.tpkprav.dto.TokenRequest;
import org.tpkprav.dto.TokenResponse;
import org.tpkprav.logging.AsyncLogger;
import org.tpkprav.service.JwtService;

import java.time.temporal.ChronoUnit;
import java.util.Map;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private static final AsyncLogger log = AsyncLogger.of(AuthController.class);

    @Inject
    JwtService jwtService;

    @Inject
    RequestContext requestContext;

    @ConfigProperty(name = "jwt.expiration-minutes")
    long expirationMinutes;

    @POST
    @Path("/token")
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, retryOn = IllegalStateException.class)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5, delayUnit = ChronoUnit.SECONDS, successThreshold = 2)
    @Bulkhead(value = 20, waitingTaskQueue = 50)
    @Fallback(value = TokenFallbackHandler.class, skipOn = {jakarta.validation.ConstraintViolationException.class})
    public TokenResponse token(@Valid TokenRequest request) {
        String reqId = requestContext.getRequestId();
        log.info("requestId={} Issuing access token nric={} uuid={}", reqId, maskNric(request.nric()), request.uuid());
        String accessToken = jwtService.createToken(Map.of(
                "nric", request.nric(),
                "uuid", request.uuid()
        ));
        long expiresIn = expirationMinutes * 60;
        log.debug("requestId={} Access token issued expiresIn={}s", reqId, expiresIn);
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }

    static String maskNric(String nric) {
        if (nric == null || nric.length() < 4) {
            return "****";
        }
        return "*".repeat(nric.length() - 4) + nric.substring(nric.length() - 4);
    }
}
