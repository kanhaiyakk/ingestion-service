package com.intentwise.ingestion.engine;

/** Wraps any failure that aborts an ingestion run after it's been marked FAILED. */
public class IngestionEngineException extends RuntimeException {

    public IngestionEngineException(String message) {
        super(message);
    }

    public IngestionEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
