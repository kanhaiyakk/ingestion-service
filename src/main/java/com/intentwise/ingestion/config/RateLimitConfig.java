package com.intentwise.ingestion.config;

/**
 * Caps the rate at which requests are issued to a source. Absent when a
 * source has no rate limit.
 */
public record RateLimitConfig(Double requestsPerSecond) {
}
