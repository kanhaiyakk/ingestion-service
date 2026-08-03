package com.intentwise.ingestion.extract;

import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.http.FetchResponse;
import java.util.List;

/** Pulls individual records out of a page response body. */
public interface RecordExtractor {

    List<ExtractedRecord> extract(FetchResponse response, ExtractConfig cfg);
}
