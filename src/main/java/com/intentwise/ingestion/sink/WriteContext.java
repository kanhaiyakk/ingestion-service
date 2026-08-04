package com.intentwise.ingestion.sink;

import com.intentwise.ingestion.extract.ExtractedRecord;
import java.util.List;

/** Records to persist for one page write: which source, which run, and the extracted records themselves. */
public record WriteContext(String sourceName, long runId, List<ExtractedRecord> records) {
}
