package com.intentwise.ingestion.config;

/**
 * Locates records within a fetched response body. Both paths are JSONPath-ish
 * expressions interpreted by {@code RecordExtractor}: {@code recordsPath}
 * points at the array of records, {@code idPath} points at the field within
 * each record used as its idempotency key.
 */
public record ExtractConfig(String recordsPath, String idPath) {
}
