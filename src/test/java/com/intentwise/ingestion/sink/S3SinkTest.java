package com.intentwise.ingestion.sink;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.extract.ExtractedRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link S3Sink} + {@link LocalFileObjectStoreClient}: type,
 * a page's records land under the run's prefix, multiple pages of the same
 * run don't overwrite each other, and an empty page writes nothing. Pure
 * local filesystem — no network, no containers.
 */
class S3SinkTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper();
    private S3Sink sink;

    @BeforeEach
    void setUp() {
        LocalFileObjectStoreClient client = new LocalFileObjectStoreClient(tempDir.toString());
        sink = new S3Sink(client, mapper, "test-bucket");
    }

    @Test
    void typeIsS3() {
        assertThat(sink.type()).isEqualTo("s3");
    }

    @Test
    void writePersistsRecordsUnderTheRunsPrefix() throws Exception {
        List<ExtractedRecord> records = List.of(
                new ExtractedRecord("a", mapper.readTree("{\"name\":\"alpha\"}")),
                new ExtractedRecord("b", mapper.readTree("{\"name\":\"bravo\"}")));

        sink.write(new WriteContext("pokeapi", 7L, records));

        Path runPrefix = tempDir.resolve("test-bucket").resolve("pokeapi").resolve("run-7");
        List<Path> written = listJsonFiles(runPrefix);
        assertThat(written).hasSize(1);

        JsonNode body = mapper.readTree(written.get(0).toFile());
        assertThat(body).hasSize(2);
        assertThat(body.get(0).get("key").asText()).isEqualTo("a");
        assertThat(body.get(0).get("payload").get("name").asText()).isEqualTo("alpha");
        assertThat(body.get(1).get("key").asText()).isEqualTo("b");
    }

    @Test
    void multiplePagesOfTheSameRunAccumulateInsteadOfOverwriting() throws Exception {
        // The engine calls write() once per page. A fixed per-run key would
        // have each page's write clobber the previous one's object — this
        // pins the fix: every page lands as its own object under the prefix.
        sink.write(new WriteContext("breweries", 3L, List.of(new ExtractedRecord("p1", mapper.readTree("{}")))));
        sink.write(new WriteContext("breweries", 3L, List.of(new ExtractedRecord("p2", mapper.readTree("{}")))));

        Path runPrefix = tempDir.resolve("test-bucket").resolve("breweries").resolve("run-3");
        List<Path> written = listJsonFiles(runPrefix);
        assertThat(written).hasSize(2);

        List<String> keysAcrossAllObjects = written.stream()
                .flatMap(path -> readKeys(path).stream())
                .toList();
        assertThat(keysAcrossAllObjects).containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void writeSkipsCreatingAnObjectWhenThereAreNoRecords() {
        sink.write(new WriteContext("pokeapi", 8L, List.of()));

        assertThat(tempDir.resolve("test-bucket")).doesNotExist();
    }

    private List<Path> listJsonFiles(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(".json")).toList();
        }
    }

    private List<String> readKeys(Path file) {
        try {
            JsonNode body = mapper.readTree(file.toFile());
            return List.of(body.get(0).get("key").asText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
