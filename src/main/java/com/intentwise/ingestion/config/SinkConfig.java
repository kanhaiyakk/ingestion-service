package com.intentwise.ingestion.config;

import java.util.Map;

/**
 * Describes where extracted records are persisted. {@code type} selects the
 * {@code Sink} implementation; {@code table} and {@code options} are
 * interpreted by that strategy.
 */
public record SinkConfig(String type, String table, Map<String, String> options) {
}
