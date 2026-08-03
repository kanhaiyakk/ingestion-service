package com.intentwise.ingestion.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.http.FetchResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link JsonPathRecordExtractor}: path selection, key derivation, and graceful edge cases. */
class JsonPathRecordExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonPathRecordExtractor extractor = new JsonPathRecordExtractor(mapper);

    @Test
    void extractsRecordsAtNestedRecordsPath() throws Exception {
        FetchResponse response = jsonResponse("{\"results\":[{\"name\":\"bulbasaur\"},{\"name\":\"ivysaur\"}]}");
        ExtractConfig cfg = new ExtractConfig("$.results", "$.name");

        List<ExtractedRecord> records = extractor.extract(response, cfg);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).key()).isEqualTo("bulbasaur");
        assertThat(records.get(1).key()).isEqualTo("ivysaur");
    }

    @Test
    void extractsTopLevelArrayWhenRecordsPathAbsent() throws Exception {
        FetchResponse response = jsonResponse("[{\"name\":\"bulbasaur\"}]");
        ExtractConfig cfg = new ExtractConfig(null, "$.name");

        List<ExtractedRecord> records = extractor.extract(response, cfg);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).key()).isEqualTo("bulbasaur");
    }

    @Test
    void fallsBackToStableHashWhenIdPathAbsentAndIsDeterministic() throws Exception {
        FetchResponse response = jsonResponse("[{\"name\":\"bulbasaur\"}]");
        ExtractConfig cfg = new ExtractConfig(null, null);

        String keyFirstRun = extractor.extract(response, cfg).get(0).key();
        String keySecondRun = extractor.extract(response, cfg).get(0).key();

        assertThat(keyFirstRun).isNotBlank().isEqualTo(keySecondRun);
    }

    @Test
    void fallsBackToHashPerRecordWhenIdPathMissingOnSomeRecords() throws Exception {
        FetchResponse response = jsonResponse("[{\"name\":\"bulbasaur\"},{\"other\":\"no-name-field\"}]");
        ExtractConfig cfg = new ExtractConfig(null, "$.name");

        List<ExtractedRecord> records = extractor.extract(response, cfg);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).key()).isEqualTo("bulbasaur");
        assertThat(records.get(1).key()).isNotBlank().isNotEqualTo("bulbasaur");
    }

    @Test
    void returnsEmptyListForEmptyArray() throws Exception {
        FetchResponse response = jsonResponse("[]");

        assertThat(extractor.extract(response, new ExtractConfig(null, null))).isEmpty();
    }

    @Test
    void returnsEmptyListWhenRecordsPathResolvesToNonArray() throws Exception {
        FetchResponse response = jsonResponse("{\"results\":{\"not\":\"an-array\"}}");
        ExtractConfig cfg = new ExtractConfig("$.results", null);

        assertThat(extractor.extract(response, cfg)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenResponseBodyIsNull() {
        FetchResponse response = new FetchResponse(204, Map.of(), null, "");

        assertThat(extractor.extract(response, new ExtractConfig(null, null))).isEmpty();
    }

    private FetchResponse jsonResponse(String json) throws Exception {
        JsonNode body = mapper.readTree(json);
        return new FetchResponse(200, Map.of(), body, json);
    }
}
