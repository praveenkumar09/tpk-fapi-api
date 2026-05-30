package org.tpkprav.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.tpkprav.service.JwtService;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.MalformedTokenException;

import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JwtServiceImpl implements JwtService {

    @ConfigProperty(name = "jwt.secret")
    String secret;

    @ConfigProperty(name = "jwt.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.expiration-minutes")
    long expirationMinutes;

    private byte[] secretBytes;

    @PostConstruct
    void init() {
        secretBytes = Base64.getDecoder().decode(secret);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 32 bytes for HS256; got " + secretBytes.length);
        }
    }

    @Override
    public String createToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString());
        claims.forEach(builder::claim);

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        try {
            jwt.sign(new MACSigner(secretBytes));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
        return jwt.serialize();
    }

    @Override
    public JWTClaimsSet verifyToken(String token) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new MalformedTokenException("Token is not a valid JWT", e);
        }

        try {
            if (!jwt.verify(new MACVerifier(secretBytes))) {
                throw new BadSignatureException("Token signature does not match");
            }
        } catch (JOSEException e) {
            throw new BadSignatureException("Token signature verification failed", e);
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new MalformedTokenException("Token claims are not parseable", e);
        }

        Date expiration = claims.getExpirationTime();
        if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
            throw new ExpiredTokenException("Token is expired");
        }
        return claims;
    }
}
