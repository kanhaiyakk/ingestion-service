package com.intentwise.ingestion.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Cursor}: follows an absolute next-URL, stops on null/absent, respects maxPages. */
class CursorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Cursor paginator = new Cursor(mapper);

    @Test
    void firstCallReturnsBaseRequest() {
        SourceConfig source = TestSources.withPagination(pagination("$.next", null));

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, null, 0));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items");
    }

    @Test
    void followsAbsoluteNextUrlFromCursorPath() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination("$.next", null));
        FetchResponse lastResponse = jsonResponse("{\"next\":\"https://example.com/api/items?offset=20\"}");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items?offset=20");
        assertThat(result.get().query()).isEmpty();
    }

    @Test
    void stopsWhenCursorValueIsExplicitJsonNull() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination("$.next", null));
        FetchResponse lastResponse = jsonResponse("{\"next\":null}");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void stopsWhenCursorPathAbsent() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, null));
        FetchResponse lastResponse = jsonResponse("{\"next\":\"https://example.com/api/items?offset=20\"}");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void maxPagesCapsAnOtherwiseInfiniteCursorSequence() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination("$.next", 3));
        FetchResponse alwaysHasNext = jsonResponse("{\"next\":\"https://example.com/api/items?page=next\"}");

        int fetched = 0;
        Optional<PageRequest> request = paginator.next(new PaginationContext(source, null, fetched));
        while (request.isPresent()) {
            fetched++;
            request = paginator.next(new PaginationContext(source, alwaysHasNext, fetched));
        }

        assertThat(fetched).isEqualTo(3);
    }

    private static PaginationConfig pagination(String cursorPath, Integer maxPages) {
        return new PaginationConfig("cursor", null, null, null, null, cursorPath, maxPages);
    }

    private FetchResponse jsonResponse(String json) throws Exception {
        JsonNode body = mapper.readTree(json);
        return new FetchResponse(200, Map.of(), body, json);
    }
}
