package com.intentwise.ingestion.pagination;

import com.intentwise.ingestion.http.PageRequest;
import java.util.Optional;

/** Produces the next page request for a source, or empty when pagination is done. */
public interface Paginator {

    String type();

    Optional<PageRequest> next(PaginationContext ctx);
}
