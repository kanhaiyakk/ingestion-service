package com.intentwise.ingestion.auth;

/** Thrown for unresolvable auth configuration: an unknown type, or a missing secret/destination. */
public class AuthConfigException extends RuntimeException {

    public AuthConfigException(String message) {
        super(message);
    }

    public AuthConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
