package com.intentwise.ingestion.http;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * The result of one page fetch: status code, response headers (keyed
 * case-insensitively), the body parsed as JSON when possible, and the raw
 * body text for callers that need it verbatim.
 */
public record FetchResponse(int status, Map<String, String> headers, JsonNode body, String rawBody) {
}
