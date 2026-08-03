package com.intentwise.ingestion.http;

/**
 * Fetches a single page of a source's HTTP API. Implementations own
 * cross-cutting concerns (timeouts, retries, rate limiting); callers just
 * supply a {@link PageRequest} and get back a {@link FetchResponse} or an
 * {@link IngestionHttpException}.
 */
public interface HttpFetcher {

    FetchResponse fetch(PageRequest request);
}
