package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.FetchResponse;

/**
 * State threaded through each {@link Paginator#next} call: the source being
 * paged, the previous fetch's response (null before the first call), and how
 * many pages have been fetched so far.
 */
public record PaginationContext(SourceConfig source, FetchResponse lastResponse, int pagesFetched) {
}
