package com.intentwise.ingestion.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.core.functions.Either;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Fetches pages over HTTP using Spring's {@link RestClient}, wrapped with a
 * connect/read timeout and a Resilience4j {@link Retry}.
 *
 * <p>Retries on I/O failures (connect timeouts, read timeouts, connection
 * resets), HTTP 429, and 5xx, with exponential backoff starting at 500ms.
 * Other 4xx responses are returned as-is, never retried. When a 429 response
 * carries a {@code Retry-After} header, that value overrides the computed
 * backoff for the next attempt. Once retries are exhausted, fetch throws
 * {@link IngestionHttpException}.
 */
@Component
public class RestClientHttpFetcher implements HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(RestClientHttpFetcher.class);

    private static final int MAX_ATTEMPTS = 5;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Duration baseBackoff;
    private final Retry retry;

    @Autowired
    public RestClientHttpFetcher(ObjectMapper objectMapper,
            @Value("${ingestion.http.timeout:10s}") Duration timeout) {
        this(objectMapper, timeout, Duration.ofMillis(500));
    }

    /**
     * Package-private constructor letting tests shrink the backoff (default
     * 500ms base, doubling over up to 5 attempts) so retry-exhaustion tests
     * don't have to sleep through it in real time.
     */
    RestClientHttpFetcher(ObjectMapper objectMapper, Duration timeout, Duration baseBackoff) {
        this.objectMapper = objectMapper;
        this.baseBackoff = baseBackoff;
        this.restClient = RestClient.builder()
                .requestFactory(buildRequestFactory(timeout))
                .build();
        this.retry = buildRetry();
    }

    @Override
    public FetchResponse fetch(PageRequest request) {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicReference<FetchResponse> lastResponse = new AtomicReference<>();

        Supplier<FetchResponse> supplier = () -> executeOnce(request, attempts.incrementAndGet(), lastResponse);
        Supplier<FetchResponse> decorated = Retry.decorateSupplier(retry, supplier);

        try {
            return decorated.get();
        } catch (MaxRetriesExceededException e) {
            FetchResponse response = lastResponse.get();
            throw new IngestionHttpException("Exhausted " + MAX_ATTEMPTS + " attempts fetching " + request.url()
                    + "; last status " + (response != null ? response.status() : "unknown"), e);
        } catch (RuntimeException e) {
            throw new IngestionHttpException("Failed to fetch " + request.url() + " after " + attempts.get()
                    + " attempt(s): " + e.getMessage(), e);
        }
    }

    private FetchResponse executeOnce(PageRequest request, int attempt, AtomicReference<FetchResponse> lastResponse) {
        try {
            FetchResponse response = doExchange(request);
            lastResponse.set(response);
            if (attempt < MAX_ATTEMPTS && isRetryableStatus(response.status())) {
                log.warn("Retrying {} (attempt {}/{}): HTTP {}", request.url(), attempt, MAX_ATTEMPTS, response.status());
            }
            return response;
        } catch (RuntimeException e) {
            if (attempt < MAX_ATTEMPTS && isRetryableException(e)) {
                log.warn("Retrying {} (attempt {}/{}) after error: {}", request.url(), attempt, MAX_ATTEMPTS, e.toString());
            }
            throw e;
        }
    }

    private FetchResponse doExchange(PageRequest request) {
        URI uri = buildUri(request);
        return restClient.get()
                .uri(uri)
                .headers(httpHeaders -> {
                    if (request.headers() != null) {
                        request.headers().forEach(httpHeaders::add);
                    }
                })
                .exchange((req, resp) -> {
                    int status = resp.getStatusCode().value();
                    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    resp.getHeaders().forEach((name, values) -> headers.put(name, String.join(", ", values)));
                    byte[] bytes = resp.bodyTo(byte[].class);
                    String rawBody = bytes == null ? "" : new String(bytes, charsetOf(headers));
                    JsonNode body = parseBody(rawBody);
                    return new FetchResponse(status, headers, body, rawBody);
                });
    }

    private JsonNode parseBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (IOException e) {
            return null;
        }
    }

    private static Charset charsetOf(Map<String, String> headers) {
        String contentType = headers.get("Content-Type");
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset != null ? charset : StandardCharsets.UTF_8;
        } catch (InvalidMediaTypeException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static URI buildUri(PageRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(request.url());
        if (request.query() != null) {
            request.query().forEach(builder::queryParam);
        }
        return builder.build().encode().toUri();
    }

    private static ClientHttpRequestFactory buildRequestFactory(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return factory;
    }

    private Retry buildRetry() {
        RetryConfig config = RetryConfig.<FetchResponse>custom()
                .maxAttempts(MAX_ATTEMPTS)
                .retryOnResult(response -> isRetryableStatus(response.status()))
                .retryOnException(RestClientHttpFetcher::isRetryableException)
                .failAfterMaxAttempts(true)
                .intervalBiFunction(this::computeBackoffMillis)
                .build();
        return Retry.of("http-fetcher", config);
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || (status >= 500 && status < 600);
    }

    private static boolean isRetryableException(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private long computeBackoffMillis(int attempt, Either<Throwable, FetchResponse> either) {
        if (either.isRight()) {
            FetchResponse response = either.get();
            if (response.status() == 429) {
                Long retryAfterMillis = parseRetryAfterMillis(response.headers());
                if (retryAfterMillis != null) {
                    return retryAfterMillis;
                }
            }
        }
        return baseBackoff.toMillis() * (1L << (attempt - 1));
    }

    private static Long parseRetryAfterMillis(Map<String, String> headers) {
        String retryAfter = headers.get("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(retryAfter.trim())).toMillis();
        } catch (NumberFormatException e) {
            try {
                ZonedDateTime when = ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                return Math.max(0, Duration.between(ZonedDateTime.now(when.getZone()), when).toMillis());
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }
}
