package com.intentwise.ingestion.config;

import java.util.Map;

/**
 * Static query parameters and headers merged into every request for a
 * source, in addition to whatever the pagination and auth strategies add.
 */
public record RequestConfig(Map<String, String> queryParams, Map<String, String> headers) {
}