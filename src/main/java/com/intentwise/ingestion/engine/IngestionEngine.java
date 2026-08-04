package com.intentwise.ingestion.engine;

import com.intentwise.ingestion.run.IngestionRun;

/**
 * Runs a full ingestion for a configured source: paginate, authenticate,
 * fetch, extract, and write.
 *
 * <p>{@code start}/{@code execute} split {@code run} into its two phases —
 * create the tracking row, then do the work — so a caller (the REST API) can
 * dispatch the work to a background executor while still returning the
 * created run's id immediately. {@code run} itself stays a synchronous,
 * single-call convenience that does both in sequence.
 */
public interface IngestionEngine {

    IngestionRun run(String sourceName);

    IngestionRun start(String sourceName);

    void execute(IngestionRun run);
}
