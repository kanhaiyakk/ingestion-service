package com.intentwise.ingestion.engine;

import com.intentwise.ingestion.run.IngestionRun;

/** Runs a full ingestion for a configured source: paginate, authenticate, fetch, extract, and write. */
public interface IngestionEngine {

    IngestionRun run(String sourceName);
}
