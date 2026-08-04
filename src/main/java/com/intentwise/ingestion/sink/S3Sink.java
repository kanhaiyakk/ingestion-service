package com.intentwise.ingestion.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writes each page's records as its own JSON object under a shared
 * {@code source/run-<id>/} prefix in an object store bucket, via
 * {@link ObjectStoreClient} — the engine calls {@code write} once per page,
 * so a run's full record set is the union of every object under its prefix,
 * not a single object (a fixed per-run key would have each page's write
 * overwrite the last). Like {@link PostgresSink}, the bucket is a single
 * shared one configured for the whole app, not per-source —
 * {@link WriteContext} only carries source name, run id, and records, not
 * the source's full {@code SinkConfig}.
 */
@Component
public class S3Sink implements Sink {

    private static final Logger log = LoggerFactory.getLogger(S3Sink.class);

    private final ObjectStoreClient client;
    private final ObjectMapper objectMapper;
    private final String bucket;

    public S3Sink(ObjectStoreClient client, ObjectMapper objectMapper,
            @Value("${ingestion.object-store.bucket:ingestion-records}") String bucket) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
    }

    @Override
    public String type() {
        return "s3";
    }

    @Override
    public void write(WriteContext ctx) {
        if (ctx.records().isEmpty()) {
            return;
        }
        String key = ctx.sourceName() + "/run-" + ctx.runId() + "/" + UUID.randomUUID() + ".json";
        client.put(bucket, key, toJson(ctx));
        log.info("Wrote {} record(s) to s3://{}/{}", ctx.records().size(), bucket, key);
    }

    private byte[] toJson(WriteContext ctx) {
        try {
            return objectMapper.writeValueAsBytes(ctx.records());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize records for run " + ctx.runId(), e);
        }
    }
}
