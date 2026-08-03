package com.intentwise.ingestion.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.PageRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Increments a page-number query parameter until a page comes back short.
 * Counting records in the last page reuses the same records-array selection
 * rule {@code extract}'s {@code JsonPathRecordExtractor} applies (records at
 * {@code extract.recordsPath}, or the whole body when that's absent and the
 * body is already an array) — duplicated here rather than shared, so this
 * package and {@code extract} stay independent.
 */
@Component
public class PageNumber implements Paginator {

    private final JsonPathReader jsonPathReader;

    public PageNumber(ObjectMapper objectMapper) {
        this.jsonPathReader = new JsonPathReader(objectMapper);
    }

    @Override
    public String type() {
        return "page_number";
    }

    @Override
    public Optional<PageRequest> next(PaginationContext ctx) {
        if (PageRequests.reachedMaxPages(ctx)) {
            return Optional.empty();
        }

        PaginationConfig cfg = ctx.source().pagination();
        int startPage = cfg.startPage() != null ? cfg.startPage() : 1;

        if (ctx.pagesFetched() == 0) {
            return Optional.of(withPage(ctx, cfg, startPage));
        }

        int lastCount = countRecords(ctx.lastResponse(), ctx.source().extract());
        if (lastCount == 0 || (cfg.pageSize() != null && lastCount < cfg.pageSize())) {
            return Optional.empty();
        }

        int nextPage = startPage + ctx.pagesFetched();
        return Optional.of(withPage(ctx, cfg, nextPage));
    }

    private static PageRequest withPage(PaginationContext ctx, PaginationConfig cfg, int page) {
        PageRequest base = PageRequests.base(ctx.source());
        Map<String, String> query = new LinkedHashMap<>(base.query());
        if (cfg.pageParam() != null) {
            query.put(cfg.pageParam(), String.valueOf(page));
        }
        if (cfg.sizeParam() != null && cfg.pageSize() != null) {
            query.put(cfg.sizeParam(), String.valueOf(cfg.pageSize()));
        }
        return new PageRequest(base.url(), query, base.headers());
    }

    private int countRecords(FetchResponse lastResponse, ExtractConfig extract) {
        if (lastResponse == null || lastResponse.body() == null) {
            return 0;
        }
        JsonNode body = lastResponse.body();
        String recordsPath = extract != null ? extract.recordsPath() : null;

        JsonNode records;
        if (recordsPath == null || recordsPath.isBlank()) {
            records = body.isArray() ? body : null;
        } else {
            JsonNode node = jsonPathReader.read(body, recordsPath);
            records = (node != null && node.isArray()) ? node : null;
        }
        return records != null ? records.size() : 0;
    }
}
