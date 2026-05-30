package org.tpkprav.service.exception;

public abstract sealed class InvalidTokenException extends RuntimeException
        permits ExpiredTokenException, BadSignatureException, MalformedTokenException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
