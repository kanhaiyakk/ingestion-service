package com.intentwise.ingestion.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.extract.ExtractedRecord;
import com.intentwise.ingestion.persistence.RawRecordRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Upserts every extracted record into Postgres via {@link RawRecordRepository}. */
@Component
public class PostgresSink implements Sink {

    private final RawRecordRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresSink(RawRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String type() {
        return "postgres";
    }

    @Override
    @Transactional
    public void write(WriteContext ctx) {
        Instant ingestedAt = Instant.now();
        for (ExtractedRecord record : ctx.records()) {
            repository.upsert(ctx.sourceName(), record.key(), toJson(record), ctx.runId(), ingestedAt);
        }
    }

    private String toJson(ExtractedRecord record) {
        try {
            return objectMapper.writeValueAsString(record.payload());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload for record '" + record.key() + "'", e);
        }
    }
}
