package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.http.PageRequest;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Follows the {@code rel="next"} link from an RFC 5988 {@code Link} response header. */
@Component
public class LinkHeader implements Paginator {

    private static final Pattern LINK_VALUE = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"?([^\";]+)\"?.*");

    @Override
    public String type() {
        return "link_header";
    }

    @Override
    public Optional<PageRequest> next(PaginationContext ctx) {
        if (PageRequests.reachedMaxPages(ctx)) {
            return Optional.empty();
        }
        if (ctx.pagesFetched() == 0) {
            return Optional.of(PageRequests.base(ctx.source()));
        }

        if (ctx.lastResponse() == null || ctx.lastResponse().headers() == null) {
            return Optional.empty();
        }

        String next = parseNext(ctx.lastResponse().headers().get("Link"));
        if (next == null) {
            return Optional.empty();
        }

        PageRequest base = PageRequests.base(ctx.source());
        return Optional.of(new PageRequest(next, Map.of(), base.headers()));
    }

    private static String parseNext(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        // Split on a comma that starts a new "<url>; rel=..." value, not on
        // commas that could appear inside a link-value's parameters.
        for (String part : linkHeader.split(",(?=\\s*<)")) {
            Matcher matcher = LINK_VALUE.matcher(part.trim());
            if (matcher.matches() && "next".equalsIgnoreCase(matcher.group(2).trim())) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
