package com.intentwise.ingestion.sink;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.extract.ExtractedRecord;
import com.intentwise.ingestion.persistence.RawRecord;
import com.intentwise.ingestion.persistence.RawRecordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests {@link PostgresSink} against a real Postgres (Testcontainers):
 * writing persists records, and re-writing the same page (a re-run) upserts
 * rather than duplicating.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostgresSinkTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private RawRecordRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private PostgresSink sink;

    @BeforeEach
    void setUp() {
        sink = new PostgresSink(repository, mapper);
    }

    @Test
    void typeIsPostgres() {
        assertThat(sink.type()).isEqualTo("postgres");
    }

    @Test
    void writePersistsEveryExtractedRecord() throws Exception {
        List<ExtractedRecord> records = List.of(
                new ExtractedRecord("bulbasaur", mapper.readTree("{\"name\":\"bulbasaur\"}")),
                new ExtractedRecord("ivysaur", mapper.readTree("{\"name\":\"ivysaur\"}")));

        sink.write(new WriteContext("pokeapi", 1L, records));

        assertThat(repository.findBySource("pokeapi")).hasSize(2);
    }

    @Test
    void reRunningTheSamePageUpsertsInsteadOfDuplicating() throws Exception {
        JsonNode payload = mapper.readTree("{\"name\":\"bulbasaur\",\"hp\":45}");
        List<ExtractedRecord> records = List.of(new ExtractedRecord("bulbasaur", payload));

        sink.write(new WriteContext("pokeapi", 1L, records));
        sink.write(new WriteContext("pokeapi", 2L, records));

        List<RawRecord> saved = repository.findBySource("pokeapi");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getRunId()).isEqualTo(2L);
    }
}
