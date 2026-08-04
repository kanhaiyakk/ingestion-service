package com.intentwise.ingestion.run;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Persistence tests for {@link IngestionRunRepository} against a real Postgres (Testcontainers). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IngestionRunRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private IngestionRunRepository repository;

    @Test
    void savesAndReloadsARunWithStatusTransitionsAndCounters() {
        IngestionRun run = new IngestionRun("pokeapi", IngestionRunStatus.RUNNING, Instant.now());
        IngestionRun saved = repository.saveAndFlush(run);

        saved.setStatus(IngestionRunStatus.SUCCESS);
        saved.setRecordsWritten(42);
        saved.setPagesFetched(3);
        saved.setFinishedAt(Instant.now());
        repository.saveAndFlush(saved);

        IngestionRun reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getSource()).isEqualTo("pokeapi");
        assertThat(reloaded.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(reloaded.getRecordsWritten()).isEqualTo(42);
        assertThat(reloaded.getPagesFetched()).isEqualTo(3);
        assertThat(reloaded.getFinishedAt()).isNotNull();
    }

    @Test
    void persistsFailedRunWithErrorMessage() {
        IngestionRun run = new IngestionRun("pokeapi", IngestionRunStatus.RUNNING, Instant.now());
        IngestionRun saved = repository.saveAndFlush(run);

        saved.setStatus(IngestionRunStatus.FAILED);
        saved.setErrorMessage("connection reset");
        repository.saveAndFlush(saved);

        IngestionRun reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(IngestionRunStatus.FAILED);
        assertThat(reloaded.getErrorMessage()).isEqualTo("connection reset");
    }
}
