package com.intentwise.ingestion.http;

import com.intentwise.ingestion.config.RateLimitConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Decorates an {@link HttpFetcher} with a rate limiter driven by a source's
 * {@link RateLimitConfig#requestsPerSecond()}, blocking the calling thread
 * until a permit is available before each fetch. Since {@code fetch}'s
 * signature carries no source identity, callers build one instance per
 * source (typically once, when the engine starts a run) via {@link #wrap}.
 */
public final class RateLimitingHttpFetcher implements HttpFetcher {

    private final HttpFetcher delegate;
    private final RateLimiter rateLimiter;

    private RateLimitingHttpFetcher(HttpFetcher delegate, RateLimiter rateLimiter) {
        this.delegate = delegate;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Wraps {@code delegate} with a rate limiter when {@code rateLimitConfig}
     * specifies a positive {@code requestsPerSecond}; otherwise returns
     * {@code delegate} unchanged, so rate limiting stays fully optional.
     *
     * <p>Modeled as one permit per {@code 1 / requestsPerSecond} refresh
     * period, rather than N permits per fixed 1-second window. That maps
     * fractional rates (e.g. {@code 0.5} -> one permit every 2s) exactly
     * instead of rounding them down to zero, and evenly paces requests
     * instead of allowing a burst of N at the top of each window.
     */
    public static HttpFetcher wrap(HttpFetcher delegate, RateLimitConfig rateLimitConfig) {
        if (rateLimitConfig == null || rateLimitConfig.requestsPerSecond() == null
                || rateLimitConfig.requestsPerSecond() <= 0) {
            return delegate;
        }

        double requestsPerSecond = rateLimitConfig.requestsPerSecond();
        Duration refreshPeriod = Duration.ofNanos(Math.round(1_000_000_000.0 / requestsPerSecond));
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(refreshPeriod)
                .timeoutDuration(Duration.ofSeconds(30))
                .build();
        return new RateLimitingHttpFetcher(delegate, RateLimiter.of("source-rate-limiter", config));
    }

    @Override
    public FetchResponse fetch(PageRequest request) {
        Supplier<FetchResponse> limited = RateLimiter.decorateSupplier(rateLimiter, () -> delegate.fetch(request));
        return limited.get();
    }
}
