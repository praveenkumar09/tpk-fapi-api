package org.tpkprav.service;

import com.nimbusds.jwt.JWTClaimsSet;

import java.util.Map;

public interface JwtService {

    String createToken(Map<String, Object> claims);

    JWTClaimsSet verifyToken(String token);
}
