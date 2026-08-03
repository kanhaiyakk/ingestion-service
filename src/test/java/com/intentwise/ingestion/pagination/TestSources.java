package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SourceConfig;

/** Builds minimal {@link SourceConfig} fixtures for pagination tests. */
final class TestSources {

    private TestSources() {
    }

    static SourceConfig withPagination(PaginationConfig pagination) {
        return withPagination(pagination, null);
    }

    static SourceConfig withPagination(PaginationConfig pagination, ExtractConfig extract) {
        return new SourceConfig("test-source", "https://example.com/api", "/items", null, null, pagination, extract, null, null);
    }
}
