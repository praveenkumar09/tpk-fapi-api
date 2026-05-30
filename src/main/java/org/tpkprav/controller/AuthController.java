package org.tpkprav.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.tpkprav.dto.TokenRequest;
import org.tpkprav.dto.TokenResponse;
import org.tpkprav.service.JwtService;

import java.util.Map;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    JwtService jwtService;

    @ConfigProperty(name = "jwt.expiration-minutes")
    long expirationMinutes;

    @POST
    @Path("/token")
    public TokenResponse token(@Valid TokenRequest request) {
        String accessToken = jwtService.createToken(Map.of(
                "nric", request.nric(),
                "uuid", request.uuid()
        ));
        return new TokenResponse(accessToken, "Bearer", expirationMinutes * 60);
    }
}
