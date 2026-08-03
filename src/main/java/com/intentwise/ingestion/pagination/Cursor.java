package com.intentwise.ingestion.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Follows a cursor URL read from {@code pagination.cursorPath} in each
 * response (e.g. PokeAPI's {@code "next"} field). The resolved value is
 * always treated as a complete next-page URL, fetched as-is; the query from
 * {@code RequestConfig} is not reapplied since the URL already carries
 * whatever query the API wants, but static headers are still carried
 * forward.
 */
@Component
public class Cursor implements Paginator {

    private final JsonPathReader jsonPathReader;

    public Cursor(ObjectMapper objectMapper) {
        this.jsonPathReader = new JsonPathReader(objectMapper);
    }

    @Override
    public String type() {
        return "cursor";
    }

    @Override
    public Optional<PageRequest> next(PaginationContext ctx) {
        if (PageRequests.reachedMaxPages(ctx)) {
            return Optional.empty();
        }
        if (ctx.pagesFetched() == 0) {
            return Optional.of(PageRequests.base(ctx.source()));
        }

        if (ctx.lastResponse() == null || ctx.lastResponse().body() == null) {
            return Optional.empty();
        }

        String cursorPath = ctx.source().pagination().cursorPath();
        if (cursorPath == null || cursorPath.isBlank()) {
            return Optional.empty();
        }

        JsonNode cursor = jsonPathReader.read(ctx.lastResponse().body(), cursorPath);
        if (cursor == null || !cursor.isTextual() || cursor.asText().isBlank()) {
            return Optional.empty();
        }

        PageRequest base = PageRequests.base(ctx.source());
        return Optional.of(new PageRequest(cursor.asText(), Map.of(), base.headers()));
    }
}
