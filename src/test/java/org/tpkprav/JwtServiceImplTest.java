package org.tpkprav;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.tpkprav.service.JwtService;
import org.tpkprav.service.exception.BadSignatureException;
import org.tpkprav.service.exception.ExpiredTokenException;
import org.tpkprav.service.exception.MalformedTokenException;
import org.tpkprav.service.impl.JwtServiceImpl;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class JwtServiceImplTest {

    // Default test secret from application.properties
    private static final byte[] TEST_SECRET =
            Base64.getDecoder().decode("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");

    @Inject
    JwtService jwtService;

    // ── createToken ──────────────────────────────────────────────────────────

    @Test
    void createToken_returnsThreePartJwt() {
        String token = jwtService.createToken(Map.of("sub", "user1"));
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void createToken_includesCustomClaims() throws Exception {
        String token = jwtService.createToken(Map.of("nric", "S9999999Z", "uuid", "abc-123"));
        JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
        assertEquals("S9999999Z", claims.getStringClaim("nric"));
        assertEquals("abc-123", claims.getStringClaim("uuid"));
    }

    @Test
    void createToken_expirationIsInFuture() throws Exception {
        String token = jwtService.createToken(Map.of("sub", "test"));
        Date exp = SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime();
        assertNotNull(exp);
        assertTrue(exp.toInstant().isAfter(Instant.now()));
    }

    @Test
    void createToken_issuerMatchesConfig() throws Exception {
        String token = jwtService.createToken(Map.of());
        assertEquals("tpk-fapi-api", SignedJWT.parse(token).getJWTClaimsSet().getIssuer());
    }

    @Test
    void createToken_uniqueJtiEachCall() throws Exception {
        String t1 = jwtService.createToken(Map.of());
        String t2 = jwtService.createToken(Map.of());
        String jti1 = SignedJWT.parse(t1).getJWTClaimsSet().getJWTID();
        String jti2 = SignedJWT.parse(t2).getJWTClaimsSet().getJWTID();
        assertNotEquals(jti1, jti2);
    }

    // ── verifyToken ──────────────────────────────────────────────────────────

    @Test
    void verifyToken_acceptsValidToken() {
        String token = jwtService.createToken(Map.of("nric", "S1234567D"));
        JWTClaimsSet claims = jwtService.verifyToken(token);
        assertNotNull(claims);
        assertEquals("S1234567D", claims.getClaim("nric"));
    }

    @Test
    void verifyToken_throwsMalformedForGarbage() {
        assertThrows(MalformedTokenException.class,
                () -> jwtService.verifyToken("not.a.jwt.at.all"));
    }

    @Test
    void verifyToken_throwsMalformedForEmptyString() {
        assertThrows(MalformedTokenException.class,
                () -> jwtService.verifyToken(""));
    }

    @Test
    void verifyToken_throwsBadSignatureForDifferentKey() throws Exception {
        byte[] differentKey = new byte[32]; // all-zero key, guaranteed different from test key
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("tpk-fapi-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(differentKey));
        assertThrows(BadSignatureException.class,
                () -> jwtService.verifyToken(jwt.serialize()));
    }

    @Test
    void verifyToken_throwsExpiredForPastExpiration() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("tpk-fapi-api")
                .issueTime(Date.from(past.minusSeconds(900)))
                .expirationTime(Date.from(past))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(TEST_SECRET));
        assertThrows(ExpiredTokenException.class,
                () -> jwtService.verifyToken(jwt.serialize()));
    }

    @Test
    void verifyToken_throwsExpiredForMissingExpiration() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("tpk-fapi-api")
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(TEST_SECRET));
        assertThrows(ExpiredTokenException.class,
                () -> jwtService.verifyToken(jwt.serialize()));
    }

    // ── init edge case ───────────────────────────────────────────────────────

    @Test
    void init_throwsWhenSecretTooShort() throws Exception {
        JwtServiceImpl service = new JwtServiceImpl();
        // "short" encodes to 5 bytes — below the 32-byte minimum
        setField(service, "secret", Base64.getEncoder().encodeToString("short".getBytes()));
        setField(service, "issuer", "test");
        setField(service, "expirationMinutes", 15L);

        java.lang.reflect.Method initMethod = JwtServiceImpl.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        assertThrows(IllegalStateException.class, () -> {
            try {
                initMethod.invoke(service);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof IllegalStateException ise) throw ise;
                throw new RuntimeException(cause);
            }
        });
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}