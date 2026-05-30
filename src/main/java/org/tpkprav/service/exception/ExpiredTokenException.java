package org.tpkprav.service.exception;

public final class ExpiredTokenException extends InvalidTokenException {

    public ExpiredTokenException(String message) {
        super(message);
    }
}
