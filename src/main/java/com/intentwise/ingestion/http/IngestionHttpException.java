package com.intentwise.ingestion.http;

/**
 * Thrown when an {@link HttpFetcher} fetch fails: retries were exhausted, or
 * the failure was not retryable in the first place.
 */
public class IngestionHttpException extends RuntimeException {

    public IngestionHttpException(String message) {
        super(message);
    }

    public IngestionHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
