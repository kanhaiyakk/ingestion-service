package com.intentwise.ingestion.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * WireMock-backed tests for {@link RestClientHttpFetcher}: happy path,
 * retry-then-succeed, retry exhaustion, and non-retryable 4xx.
 */
class RestClientHttpFetcherTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Small base backoff keeps the retry-exhaustion test fast without changing
    // the attempt count or doubling behavior under test.
    private final RestClientHttpFetcher fetcher =
            new RestClientHttpFetcher(new ObjectMapper(), Duration.ofSeconds(2), Duration.ofMillis(5));

    @Test
    void returnsParsedBodyOn200() {
        wireMock.stubFor(get(urlEqualTo("/pokemon")).willReturn(okJson("{\"name\":\"pikachu\"}")));

        FetchResponse response = fetcher.fetch(pageRequest("/pokemon"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().get("name").asText()).isEqualTo("pikachu");
        assertThat(response.rawBody()).contains("pikachu");
        wireMock.verify(1, getRequestedFor(urlEqualTo("/pokemon")));
    }

    @Test
    void retriesTwo429sThenSucceeds() {
        wireMock.stubFor(get(urlEqualTo("/flaky")).inScenario("retry-then-succeed")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0"))
                .willSetStateTo("second-429"));
        wireMock.stubFor(get(urlEqualTo("/flaky")).inScenario("retry-then-succeed")
                .whenScenarioStateIs("second-429")
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0"))
                .willSetStateTo("succeed"));
        wireMock.stubFor(get(urlEqualTo("/flaky")).inScenario("retry-then-succeed")
                .whenScenarioStateIs("succeed")
                .willReturn(okJson("{\"ok\":true}")));

        FetchResponse response = fetcher.fetch(pageRequest("/flaky"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().get("ok").asBoolean()).isTrue();
        wireMock.verify(3, getRequestedFor(urlEqualTo("/flaky")));
    }

    @Test
    void persistentServerErrorExhaustsRetriesAndThrows() {
        wireMock.stubFor(get(urlEqualTo("/broken")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> fetcher.fetch(pageRequest("/broken")))
                .isInstanceOf(IngestionHttpException.class)
                .hasMessageContaining("500");

        wireMock.verify(5, getRequestedFor(urlEqualTo("/broken")));
    }

    @Test
    void notFoundDoesNotRetry() {
        wireMock.stubFor(get(urlEqualTo("/missing")).willReturn(aResponse().withStatus(404)));

        FetchResponse response = fetcher.fetch(pageRequest("/missing"));

        assertThat(response.status()).isEqualTo(404);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/missing")));
    }

    private PageRequest pageRequest(String path) {
        return new PageRequest(wireMock.baseUrl() + path, Map.of(), Map.of());
    }
}
