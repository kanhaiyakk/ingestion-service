package com.intentwise.ingestion.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.intentwise.ingestion.config.PaginationConfig;
import com.intentwise.ingestion.config.SourceConfig;
import com.intentwise.ingestion.http.FetchResponse;
import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LinkHeader}: follows rel="next", stops when absent. */
class LinkHeaderTest {

    private final LinkHeader paginator = new LinkHeader();
    private final SourceConfig source = TestSources.withPagination(
            new PaginationConfig("link_header", null, null, null, null, null, null));

    @Test
    void firstCallReturnsBaseRequest() {
        Optional<PageRequest> result = paginator.next(new PaginationContext(source, null, 0));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items");
    }

    @Test
    void followsRelNextLink() {
        FetchResponse lastResponse = responseWithLink("<https://example.com/api/items?page=2>; rel=\"next\"");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items?page=2");
    }

    @Test
    void stopsWhenNoNextRelPresent() {
        FetchResponse lastResponse = responseWithLink("<https://example.com/api/items?page=1>; rel=\"prev\"");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void stopsOnLastPageWherePrevAndFirstArePresentButNotNext() {
        // Real GitHub-shaped last-page header: prev + first, no next.
        FetchResponse lastResponse = responseWithLink(
                "<https://api.github.com/repos/octocat/repo/issues?page=33>; rel=\"prev\", "
                        + "<https://api.github.com/repos/octocat/repo/issues?page=1>; rel=\"first\"");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void stopsWhenLinkHeaderAbsent() {
        FetchResponse lastResponse = new FetchResponse(200, Map.of(), null, "");

        assertThat(paginator.next(new PaginationContext(source, lastResponse, 1))).isEmpty();
    }

    @Test
    void parsesMultipleLinkValuesInOneHeader() {
        FetchResponse lastResponse = responseWithLink(
                "<https://example.com/api/items?page=3>; rel=\"last\", <https://example.com/api/items?page=2>; rel=\"next\"");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://example.com/api/items?page=2");
    }

    @Test
    void picksNextSpecificallyFromARealGitHubShapedHeader() {
        // Exact GitHub middle-page shape: next listed first, last second.
        FetchResponse lastResponse = responseWithLink(
                "<https://api.github.com/repos/octocat/repo/issues?page=2>; rel=\"next\", "
                        + "<https://api.github.com/repos/octocat/repo/issues?page=34>; rel=\"last\"");

        Optional<PageRequest> result = paginator.next(new PaginationContext(source, lastResponse, 1));

        assertThat(result).isPresent();
        assertThat(result.get().url()).isEqualTo("https://api.github.com/repos/octocat/repo/issues?page=2");
    }

    private static FetchResponse responseWithLink(String linkHeader) {
        return new FetchResponse(200, Map.of("Link", linkHeader), null, "");
    }
}
