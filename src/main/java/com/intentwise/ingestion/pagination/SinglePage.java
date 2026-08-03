package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.http.PageRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Fetches exactly one page: the source's base request, then nothing further. */
@Component
public class SinglePage implements Paginator {

    @Override
    public String type() {
        return "single";
    }

    @Override
    public Optional<PageRequest> next(PaginationContext ctx) {
        if (PageRequests.reachedMaxPages(ctx) || ctx.pagesFetched() > 0) {
            return Optional.empty();
        }
        return Optional.of(PageRequests.base(ctx.source()));
    }
}
