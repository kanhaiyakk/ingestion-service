package com.intentwise.ingestion.sink;

/** Thrown for unresolvable sink configuration, e.g. an unknown type. */
public class SinkConfigException extends RuntimeException {

    public SinkConfigException(String message) {
        super(message);
    }

    public SinkConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
