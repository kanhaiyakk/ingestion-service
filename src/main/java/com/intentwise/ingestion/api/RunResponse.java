package com.intentwise.ingestion.api;

import com.intentwise.ingestion.run.IngestionRun;
import com.intentwise.ingestion.run.IngestionRunStatus;
import java.time.Instant;

/** API view of an {@link IngestionRun}; never exposes the JPA entity directly. */
public record RunResponse(
        long id,
        String source,
        IngestionRunStatus status,
        long recordsWritten,
        int pagesFetched,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage) {

    public static RunResponse from(IngestionRun run) {
        return new RunResponse(run.getId(), run.getSource(), run.getStatus(), run.getRecordsWritten(),
                run.getPagesFetched(), run.getStartedAt(), run.getFinishedAt(), run.getErrorMessage());
    }
}
