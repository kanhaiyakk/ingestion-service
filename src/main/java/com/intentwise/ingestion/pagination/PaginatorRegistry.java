package com.intentwise.ingestion.pagination;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Indexes every {@link Paginator} bean by {@link Paginator#type()}. */
@Component
public class PaginatorRegistry {

    private final Map<String, Paginator> paginatorsByType;

    public PaginatorRegistry(List<Paginator> paginators) {
        this.paginatorsByType = paginators.stream()
                .collect(Collectors.toUnmodifiableMap(Paginator::type, Function.identity()));
    }

    public Paginator get(String type) {
        Paginator paginator = paginatorsByType.get(type);
        if (paginator == null) {
            throw new PaginationConfigException("Unknown pagination type '" + type + "'; available: " + paginatorsByType.keySet());
        }
        return paginator;
    }
}
