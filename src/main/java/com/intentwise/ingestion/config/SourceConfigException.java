package com.intentwise.ingestion.config;

/**
 * Thrown when a source config file fails to parse, fails validation, or
 * collides with an already-registered source name. Loading fails fast: the
 * first invalid file or duplicate source name aborts startup with a message
 * identifying the offending file or name.
 */
public class SourceConfigException extends RuntimeException {

    public SourceConfigException(String message) {
        super(message);
    }

    public SourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
