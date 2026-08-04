package com.intentwise.ingestion.engine;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.intentwise.ingestion.auth.EnvAccessor;
import com.intentwise.ingestion.config.AuthConfig;
import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SinkConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.config.SourceRegistry;
import com.intentwise.ingestion.persistence.RawRecord;
import com.intentwise.ingestion.persistence.RawRecordRepository;
import com.intentwise.ingestion.run.IngestionRun;
import com.intentwise.ingestion.run.IngestionRunRepository;
import com.intentwise.ingestion.run.IngestionRunStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration matrix for {@link DefaultIngestionEngine}: hermetic (WireMock
 * fake APIs + Testcontainers Postgres, no real external calls), covering the
 * two axes a source's config varies on — auth (none vs. bearer) and
 * pagination (cursor vs. link-header) — plus re-run idempotency and a
 * failure path through a non-page_number pagination strategy.
 */
@SpringBootTest
@Testcontainers
class EngineIntegrationMatrixTest {

    private static final String BEARER_TOKEN_ENV = "MATRIX_TEST_TOKEN";
    private static final String BEARER_TOKEN_VALUE = "test-token-123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private IngestionEngine engine;

    @Autowired
    private SourceRegistry sourceRegistry;

    @Autowired
    private RawRecordRepository rawRecordRepository;

    @Autowired
    private IngestionRunRepository runRepository;

    /** Fake env, resolved with @Primary priority, so bearer auth can be exercised without touching real process env. */
    @TestConfiguration
    static class FakeEnvConfig {
        @Bean
        @Primary
        EnvAccessor fakeEnvAccessor() {
            return name -> BEARER_TOKEN_ENV.equals(name) ? BEARER_TOKEN_VALUE : null;
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void noAuthCursorPaginationSucceedsWithCorrectCounts() {
        String sourceName = "matrix-cursor";
        sourceRegistry.register(cursorSource(sourceName));
        stubCursorPages();

        IngestionRun result = engine.run(sourceName);

        assertThat(result.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(result.getPagesFetched()).isEqualTo(2);
        assertThat(result.getRecordsWritten()).isEqualTo(3);
        assertThat(rawRecordRepository.findBySource(sourceName))
                .extracting(RawRecord::getRecordKey)
                .containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void bearerAuthLinkHeaderPaginationSucceedsWithCorrectCounts() {
        String sourceName = "matrix-link-header";
        sourceRegistry.register(linkHeaderSource(sourceName));
        stubLinkHeaderPage1Ok();
        wireMock.stubFor(get(urlPathEqualTo("/repos")).withQueryParam("page", equalTo("2"))
                .willReturn(okJson("[{\"id\":3}]")));

        IngestionRun result = engine.run(sourceName);

        assertThat(result.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(result.getPagesFetched()).isEqualTo(2);
        assertThat(result.getRecordsWritten()).isEqualTo(3);
        assertThat(rawRecordRepository.findBySource(sourceName))
                .extracting(RawRecord::getRecordKey)
                .containsExactlyInAnyOrder("1", "2", "3");

        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/repos"))
                .withHeader("Authorization", equalTo("Bearer " + BEARER_TOKEN_VALUE)));
    }

    @Test
    void reRunningTheSameSourceUpsertsInsteadOfDuplicating() {
        String sourceName = "matrix-rerun";
        sourceRegistry.register(cursorSource(sourceName));
        stubCursorPages();

        engine.run(sourceName);
        engine.run(sourceName);

        List<IngestionRun> runs = runRepository.findBySource(sourceName);
        assertThat(runs).hasSize(2);
        assertThat(runs).allSatisfy(run -> assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS));
        assertThat(rawRecordRepository.findBySource(sourceName)).hasSize(3);
    }

    @Test
    void persistentServerErrorOnLinkHeaderPageMarksRunFailed() {
        String sourceName = "matrix-link-header-failure";
        sourceRegistry.register(linkHeaderSource(sourceName));
        stubLinkHeaderPage1Ok();
        wireMock.stubFor(get(urlPathEqualTo("/repos")).withQueryParam("page", equalTo("2"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> engine.run(sourceName)).isInstanceOf(IngestionEngineException.class);

        List<IngestionRun> runs = runRepository.findBySource(sourceName);
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getStatus()).isEqualTo(IngestionRunStatus.FAILED);
        assertThat(runs.get(0).getPagesFetched()).isEqualTo(1);
        assertThat(runs.get(0).getRecordsWritten()).isEqualTo(2);
        assertThat(rawRecordRepository.findBySource(sourceName)).hasSize(2);
    }

    private void stubCursorPages() {
        wireMock.stubFor(get(urlPathEqualTo("/items"))
                .willReturn(okJson("{\"results\":[{\"id\":\"a\"},{\"id\":\"b\"}],\"next\":\""
                        + wireMock.baseUrl() + "/items?cursor=2\"}")));
        wireMock.stubFor(get(urlPathEqualTo("/items")).withQueryParam("cursor", equalTo("2"))
                .willReturn(okJson("{\"results\":[{\"id\":\"c\"}],\"next\":null}")));
    }

    private void stubLinkHeaderPage1Ok() {
        wireMock.stubFor(get(urlPathEqualTo("/repos"))
                .willReturn(okJson("[{\"id\":1},{\"id\":2}]")
                        .withHeader("Link", "<" + wireMock.baseUrl() + "/repos?page=2>; rel=\"next\"")));
    }

    private SourceConfig cursorSource(String name) {
        return new SourceConfig(name, wireMock.baseUrl(), "/items", null, null,
                new PaginationConfig("cursor", null, null, null, null, "$.next", 10),
                new ExtractConfig("$.results", "$.id"),
                new SinkConfig("postgres", null, null), null);
    }

    private SourceConfig linkHeaderSource(String name) {
        AuthConfig bearerAuth = new AuthConfig("bearer", null, null, "Bearer", BEARER_TOKEN_ENV);
        return new SourceConfig(name, wireMock.baseUrl(), "/repos", null, bearerAuth,
                new PaginationConfig("link_header", null, null, null, null, null, 10),
                new ExtractConfig(null, "$.id"),
                new SinkConfig("postgres", null, null), null);
    }
}
