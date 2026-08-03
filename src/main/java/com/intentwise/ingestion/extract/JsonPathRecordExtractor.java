package com.intentwise.ingestion.extract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.http.FetchResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Selects the records array at {@code extract.recordsPath} (or the whole
 * body when that's absent and the body is already an array) and derives a
 * stable key per record from {@code extract.idPath}, falling back to a
 * SHA-256 hash of the record when the id field is absent or missing.
 */
@Component
public class JsonPathRecordExtractor implements RecordExtractor {

    private static final Logger log = LoggerFactory.getLogger(JsonPathRecordExtractor.class);

    private final JsonPathReader jsonPathReader;

    public JsonPathRecordExtractor(ObjectMapper objectMapper) {
        this.jsonPathReader = new JsonPathReader(objectMapper);
    }

    @Override
    public List<ExtractedRecord> extract(FetchResponse response, ExtractConfig cfg) {
        if (response == null || response.body() == null) {
            return List.of();
        }

        JsonNode records = selectRecords(response.body(), cfg);
        if (records == null) {
            return List.of();
        }

        String idPath = cfg != null ? cfg.idPath() : null;
        List<ExtractedRecord> result = new ArrayList<>(records.size());
        for (JsonNode record : records) {
            result.add(new ExtractedRecord(deriveKey(record, idPath), record));
        }
        return result;
    }

    private JsonNode selectRecords(JsonNode body, ExtractConfig cfg) {
        String recordsPath = cfg != null ? cfg.recordsPath() : null;
        if (recordsPath == null || recordsPath.isBlank()) {
            return body.isArray() ? body : null;
        }

        JsonNode node = jsonPathReader.read(body, recordsPath);
        if (node == null) {
            log.warn("extract.recordsPath '{}' did not resolve in the response body", recordsPath);
            return null;
        }
        if (!node.isArray()) {
            log.warn("extract.recordsPath '{}' resolved to a non-array value", recordsPath);
            return null;
        }
        return node;
    }

    private String deriveKey(JsonNode record, String idPath) {
        if (idPath != null && !idPath.isBlank()) {
            JsonNode idNode = jsonPathReader.read(record, idPath);
            if (idNode != null) {
                return idNode.isTextual() ? idNode.asText() : idNode.toString();
            }
        }
        return sha256Hex(record);
    }

    private static String sha256Hex(JsonNode record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(record.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
