package com.intentwise.ingestion.extract;

import com.fasterxml.jackson.databind.JsonNode;

/** One record pulled from a page response: a stable idempotency key plus its JSON payload. */
public record ExtractedRecord(String key, JsonNode payload) {
}
