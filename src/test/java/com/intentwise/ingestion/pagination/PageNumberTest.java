package com.intentwise.ingestion.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.config.ExtractConfig;
import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PageNumber}: page increment, short-page/empty-page stop conditions, maxPages. */
class PageNumberTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PageNumber paginator = new PageNumber(mapper);

    @Test
    void firstPageUsesDefaultStartPageOne() {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null));

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, null, 0));

        assertThat(result).isPresent();
        assertThat(result.get().query()).containsEntry("page", "1").containsEntry("size", "2");
    }

    @Test
    void firstPageUsesConfiguredStartPage() {
        SourceConfig source = TestSources.withPagination(pagination(5, 2, null));

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, null, 0));

        assertThat(result.get().query()).containsEntry("page", "5");
    }

    @Test
    void incrementsPageNumberWhenLastPageIsFull() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null));
        FetchResponse lastResponse = jsonResponse("[{\"id\":1},{\"id\":2}]");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().query()).containsEntry("page", "2");
    }

    @Test
    void stopsWhenLastPageHasFewerThanPageSizeRecords() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null));
        FetchResponse lastResponse = jsonResponse("[{\"id\":1}]");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void stopsWhenLastPageIsEmptyArray() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null));
        FetchResponse lastResponse = jsonResponse("[]");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void countsRecordsViaConfiguredRecordsPath() throws Exception {
        ExtractConfig extract = new ExtractConfig("$.results", null);
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null), extract);
        FetchResponse lastResponse = jsonResponse("{\"results\":[{\"id\":1},{\"id\":2}]}");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().query()).containsEntry("page", "2");
    }

    @Test
    void countsRecordsFromTopLevelArrayWhenRecordsPathAbsent() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, null));
        FetchResponse lastResponse = jsonResponse("[{\"id\":1}]");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void maxPagesCapsPagination() throws Exception {
        SourceConfig source = TestSources.withPagination(pagination(null, 2, 2));
        FetchResponse lastResponse = jsonResponse("[{\"id\":1},{\"id\":2}]");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 2))).isEmpty();
    }

    private static PaginationConfig pagination(Integer startPage, Integer pageSize, Integer maxPages) {
        return new PaginationConfig("page_number", "page", "size", pageSize, startPage, null, maxPages);
    }

    private FetchResponse jsonResponse(String json) throws Exception {
        JsonNode body = mapper.readTree(json);
        return new FetchResponse(200, Map.of(), body, json);
    }
}
