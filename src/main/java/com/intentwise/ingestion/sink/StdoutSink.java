package com.intentwise.ingestion.sink;

import com.fasterxml.jackson.databind.JsonNode;
import com.intentwise.ingestion.extract.ExtractedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs each record to stdout at INFO; useful for dry-running a new source before wiring a real sink. */
@Component
public class StdoutSink implements Sink {

    private static final Logger log = LoggerFactory.getLogger(StdoutSink.class);
    private static final int MAX_PAYLOAD_CHARS = 200;

    @Override
    public String type() {
        return "stdout";
    }

    @Override
    public void write(WriteContext ctx) {
        for (ExtractedRecord record : ctx.records()) {
            log.info("[{}] key={} payload={}", ctx.sourceName(), record.key(), truncate(record.payload()));
        }
        log.info("Wrote {} record(s) to stdout for source '{}' (run {})", ctx.records().size(), ctx.sourceName(), ctx.runId());
    }

    private static String truncate(JsonNode payload) {
        String text = payload != null ? payload.toString() : "null";
        return text.length() > MAX_PAYLOAD_CHARS ? text.substring(0, MAX_PAYLOAD_CHARS) + "..." : text;
    }
}
