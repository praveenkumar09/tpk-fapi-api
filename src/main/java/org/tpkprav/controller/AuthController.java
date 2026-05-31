package org.tpkprav.controller;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tpkprav.api.RequestContext;
import org.tpkprav.client.DbConnectorClient;
import org.tpkprav.client.dto.StoreRequest;
import org.tpkprav.client.dto.StoreResponse;
import org.tpkprav.dto.TokenRequest;
import org.tpkprav.dto.TokenResponse;
import io.smallrye.common.annotation.RunOnVirtualThread;
import org.tpkprav.logging.AsyncLogger;
import org.tpkprav.service.JwtService;
import org.tpkprav.util.MaskingUtils;

import java.time.temporal.ChronoUnit;
import java.util.Map;

@RunOnVirtualThread
@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private static final AsyncLogger log = AsyncLogger.of(AuthController.class);

    @Inject
    JwtService jwtService;

    @Inject
    RequestContext requestContext;

    @RestClient
    DbConnectorClient dbConnectorClient;

    @ConfigProperty(name = "jwt.expiration-minutes")
    long expirationMinutes;

    @POST
    @Path("/token")
    @Retry(maxRetries = 2, delay = 100, delayUnit = ChronoUnit.MILLIS, retryOn = IllegalStateException.class)
    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 5, delayUnit = ChronoUnit.SECONDS, successThreshold = 2,
            skipOn = {ConstraintViolationException.class, WebApplicationException.class})
    @Bulkhead(value = 20, waitingTaskQueue = 50)
    @Fallback(value = TokenFallbackHandler.class,
            skipOn = {ConstraintViolationException.class, WebApplicationException.class})
    public TokenResponse token(@Valid TokenRequest request) {
        String reqId = requestContext.getRequestId();
        log.info("requestId={} Token request received nric={} uuid={}",
                reqId, MaskingUtils.maskNric(request.nric()), request.uuid());

        StoreResponse stored = dbConnectorClient.store(new StoreRequest(request.nric(), request.uuid()));
        log.debug("requestId={} Credential stored status={}", reqId, stored.status());

        if (!"stored".equals(stored.status())) {
            log.warn("requestId={} Unexpected store response status={}", reqId, stored.status());
            throw new WebApplicationException("Unexpected response from credential store", Response.Status.BAD_GATEWAY);
        }

        String accessToken = jwtService.createToken(Map.of(
                "nric", request.nric(),
                "uuid", request.uuid()
        ));
        long expiresIn = expirationMinutes * 60;
        log.debug("requestId={} Access token issued expiresIn={}s", reqId, expiresIn);
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
