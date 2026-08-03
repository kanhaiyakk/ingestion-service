package com.intentwise.ingestion.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.intentwise.ingestion.config.RateLimitConfig;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RateLimitingHttpFetcher}: absent/zero config leaves
 * the fetcher unwrapped, a configured limit still delegates correctly.
 */
class RateLimitingHttpFetcherTest {

    @Test
    void noConfigReturnsDelegateUnchanged() {
        HttpFetcher delegate = request -> new FetchResponse(200, Map.of(), null, "");

        assertThat(RateLimitingHttpFetcher.wrap(delegate, null)).isSameAs(delegate);
        assertThat(RateLimitingHttpFetcher.wrap(delegate, new RateLimitConfig(null))).isSameAs(delegate);
        assertThat(RateLimitingHttpFetcher.wrap(delegate, new RateLimitConfig(0.0))).isSameAs(delegate);
    }

    @Test
    void fractionalRateStillWrapsInsteadOfFlooringToZero() {
        HttpFetcher delegate = request -> new FetchResponse(200, Map.of(), null, "");

        HttpFetcher limited = RateLimitingHttpFetcher.wrap(delegate, new RateLimitConfig(0.5));

        assertThat(limited).isNotSameAs(delegate);
        assertThat(limited.fetch(new PageRequest("https://example.com", Map.of(), Map.of())).status()).isEqualTo(200);
    }

    @Test
    void configuredLimiterStillDelegatesFetches() {
        AtomicInteger callCount = new AtomicInteger();
        HttpFetcher delegate = request -> {
            callCount.incrementAndGet();
            return new FetchResponse(200, Map.of(), null, "");
        };

        HttpFetcher limited = RateLimitingHttpFetcher.wrap(delegate, new RateLimitConfig(50.0));
        assertThat(limited).isNotSameAs(delegate);

        FetchResponse response = limited.fetch(new PageRequest("https://example.com", Map.of(), Map.of()));

        assertThat(response.status()).isEqualTo(200);
        assertThat(callCount.get()).isEqualTo(1);
    }
}
