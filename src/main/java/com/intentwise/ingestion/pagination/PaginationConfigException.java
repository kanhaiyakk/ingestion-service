package com.intentwise.ingestion.pagination;

/** Thrown for unresolvable pagination configuration, e.g. an unknown type. */
public class PaginationConfigException extends RuntimeException {

    public PaginationConfigException(String message) {
        super(message);
    }

    public PaginationConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
