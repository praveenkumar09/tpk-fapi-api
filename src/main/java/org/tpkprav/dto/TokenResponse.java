package org.tpkprav.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
