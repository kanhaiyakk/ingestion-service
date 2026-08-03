package com.intentwise.ingestion.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SinglePage}: fetches exactly one page. */
class SinglePageTest {

    private final SinglePage paginator = new SinglePage();

    @Test
    void firstCallReturnsBaseRequest() {
        SourceConfig source = TestSources.withPagination(new PaginationConfig("single", null, null, null, null, null, null));

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, null, 0));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items");
    }

    @Test
    void secondCallReturnsEmptyAfterOnePageFetched() {
        SourceConfig source = TestSources.withPagination(new PaginationConfig("single", null, null, null, null, null, null));
        FetchResponse response = new FetchResponse(200, Map.of(), null, "");

        assertThat(paginator.next(new PaginationContext(source, response, 1))).isEmpty();
    }
}
