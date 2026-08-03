package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.config.RequestConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.PageRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the starting request for a source and enforces {@code maxPages}, shared by every {@link Paginator}. */
final class PageRequests {

    private PageRequests() {
    }

    static PageRequest base(SourceConfig source) {
        String url = source.baseUrl() + (source.path() != null ? source.path() : "");
        RequestConfig request = source.request();
        Map<String, String> query = request != null && request.queryParams() != null
                ? new LinkedHashMap<>(request.queryParams())
                : new LinkedHashMap<>();
        Map<String, String> headers = request != null && request.headers() != null
                ? new LinkedHashMap<>(request.headers())
                : new LinkedHashMap<>();
        return new PageRequest(url, query, headers);
    }

    static boolean reachedMaxPages(PaginationContext ctx) {
        Integer maxPages = ctx.source().pagination().maxPages();
        return maxPages != null && ctx.pagesFetched() >= maxPages;
    }
}
