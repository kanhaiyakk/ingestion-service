package com.intentwise.ingestion.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Declarative definition of a single ingestable data source: where to fetch
 * it from, how to authenticate, how to paginate, where records live in the
 * response body, and where to write them. One YAML file under the configured
 * sources directory maps to exactly one {@code SourceConfig}.
 */
public record SourceConfig(
        @NotBlank String name,
        @NotBlank String baseUrl,
        String path,
        @Valid RequestConfig request,
        @Valid AuthConfig auth,
        @NotNull @Valid PaginationConfig pagination,
        @Valid ExtractConfig extract,
        @Valid SinkConfig sink,
        @Valid RateLimitConfig rateLimit) {

    public SourceConfig {
        if (auth == null) {
            auth = new AuthConfig(null, null, null, null, null);
        }
    }
}
