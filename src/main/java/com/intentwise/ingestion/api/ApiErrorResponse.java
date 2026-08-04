package com.intentwise.ingestion.api;

/** Small JSON error body returned by {@link ApiExceptionHandler}. */
public record ApiErrorResponse(int status, String message) {
}
