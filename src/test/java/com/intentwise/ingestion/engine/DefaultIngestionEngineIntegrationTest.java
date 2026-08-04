package com.intentwise.ingestion.engine;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end test of {@link DefaultIngestionEngine} against a fake paginated
 * API (WireMock) and a real Postgres (Testcontainers, never H2): a full run
 * lands every page's records and marks the run SUCCESS with correct counts;
 * a run against a broken page marks FAILED and rethrows, while still keeping
 * whatever earlier pages it did manage to persist.
 */
@SpringBootTest
@Testcontainers
class DefaultIngestionEngineIntegrationTest {

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

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void fullIngestionPersistsRecordsAndMarksRunSuccess() {
        String sourceName = "wiremock-success";
        sourceRegistry.register(pagedSource(sourceName));

        stubPage(1, "{\"results\":[{\"name\":\"bulbasaur\"},{\"name\":\"ivysaur\"}]}");
        stubPage(2, "{\"results\":[{\"name\":\"venusaur\"}]}");

        IngestionRun result = engine.run(sourceName);

        assertThat(result.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(result.getPagesFetched()).isEqualTo(2);
        assertThat(result.getRecordsWritten()).isEqualTo(3);
        assertThat(result.getFinishedAt()).isNotNull();

        List<RawRecord> saved = rawRecordRepository.findBySource(sourceName);
        assertThat(saved).extracting(RawRecord::getRecordKey)
                .containsExactlyInAnyOrder("bulbasaur", "ivysaur", "venusaur");

        IngestionRun reloaded = runRepository.findById(result.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
    }

    @Test
    void brokenPageMarksRunFailedButKeepsEarlierPagesPersisted() {
        String sourceName = "wiremock-failure";
        sourceRegistry.register(pagedSource(sourceName));

        stubPage(1, "{\"results\":[{\"name\":\"bulbasaur\"},{\"name\":\"ivysaur\"}]}");
        wireMock.stubFor(get(urlPathEqualTo("/pokemon"))
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("size", equalTo("2"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> engine.run(sourceName)).isInstanceOf(IngestionEngineException.class);

        List<IngestionRun> runs = runRepository.findBySource(sourceName);
        assertThat(runs).hasSize(1);
        IngestionRun run = runs.get(0);
        assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isNotBlank();
        assertThat(run.getPagesFetched()).isEqualTo(1);
        assertThat(run.getRecordsWritten()).isEqualTo(2);

        assertThat(rawRecordRepository.findBySource(sourceName)).hasSize(2);
    }

    private void stubPage(int page, String body) {
        wireMock.stubFor(get(urlPathEqualTo("/pokemon"))
                .withQueryParam("page", equalTo(String.valueOf(page)))
                .withQueryParam("size", equalTo("2"))
                .willReturn(okJson(body)));
    }

    private SourceConfig pagedSource(String name) {
        return new SourceConfig(
                name,
                wireMock.baseUrl(),
                "/pokemon",
                null,
                null,
                new PaginationConfig("page_number", "page", "size", 2, 1, null, null),
                new ExtractConfig("$.results", "$.name"),
                new SinkConfig("postgres", null, null),
                null);
    }
}
