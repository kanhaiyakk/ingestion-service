package com.intentwise.ingestion.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Persistence tests for {@link RawRecordRepository} against a real Postgres
 * (Testcontainers, never H2 — JSONB and upsert-on-conflict are
 * Postgres-specific): writing persists, re-writing the same key upserts
 * instead of duplicating, and the JSONB column round-trips nested structure.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RawRecordRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private RawRecordRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void upsertPersistsANewRecord() {
        repository.upsert("pokeapi", "bulbasaur", "{\"name\":\"bulbasaur\"}", 1L, Instant.now());

        Optional<RawRecord> saved = repository.findBySourceAndRecordKey("pokeapi", "bulbasaur");

        assertThat(saved).isPresent();
        assertThat(saved.get().getSource()).isEqualTo("pokeapi");
        assertThat(saved.get().getRunId()).isEqualTo(1L);
    }

    @Test
    void reIngestingTheSameKeyUpdatesInPlaceInsteadOfDuplicating() {
        repository.upsert("pokeapi", "bulbasaur", "{\"name\":\"bulbasaur\",\"hp\":45}", 1L, Instant.now());
        repository.upsert("pokeapi", "bulbasaur", "{\"name\":\"bulbasaur\",\"hp\":99}", 2L, Instant.now());

        List<RawRecord> all = repository.findBySource("pokeapi");

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getPayload()).contains("99");
        assertThat(all.get(0).getRunId()).isEqualTo(2L);
    }

    @Test
    void upsertIsScopedPerSourceSoSameKeyInDifferentSourcesBothPersist() {
        repository.upsert("pokeapi", "1", "{\"name\":\"bulbasaur\"}", 1L, Instant.now());
        repository.upsert("other-api", "1", "{\"name\":\"something-else\"}", 1L, Instant.now());

        assertThat(repository.findBySource("pokeapi")).hasSize(1);
        assertThat(repository.findBySource("other-api")).hasSize(1);
    }

    @Test
    void jsonbColumnRoundTripsNestedStructures() throws Exception {
        String json = "{\"name\":\"bulbasaur\",\"types\":[\"grass\",\"poison\"],\"stats\":{\"hp\":45}}";

        repository.upsert("pokeapi", "bulbasaur", json, 1L, Instant.now());

        RawRecord saved = repository.findBySourceAndRecordKey("pokeapi", "bulbasaur").orElseThrow();
        JsonNode roundTripped = mapper.readTree(saved.getPayload());

        assertThat(roundTripped.get("name").asText()).isEqualTo("bulbasaur");
        assertThat(roundTripped.get("types")).hasSize(2);
        assertThat(roundTripped.get("types").get(0).asText()).isEqualTo("grass");
        assertThat(roundTripped.get("stats").get("hp").asInt()).isEqualTo(45);
    }
}
