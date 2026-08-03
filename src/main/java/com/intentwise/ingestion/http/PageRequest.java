package com.intentwise.ingestion.http;

import java.util.Map;

/**
 * A single page fetch to perform: either an absolute URL a paginator has
 * already resolved, or a base URL combined with additional query parameters.
 * {@code headers} are merged into the request (auth strategies add to these).
 */
public record PageRequest(String url, Map<String, String> query, Map<String, String> headers) {
}
