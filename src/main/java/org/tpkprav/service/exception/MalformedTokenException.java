package org.tpkprav.service.exception;

public final class MalformedTokenException extends InvalidTokenException {

    public MalformedTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
