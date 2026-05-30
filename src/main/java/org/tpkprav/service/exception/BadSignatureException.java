package org.tpkprav.service.exception;

public final class BadSignatureException extends InvalidTokenException {

    public BadSignatureException(String message) {
        super(message);
    }

    public BadSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
