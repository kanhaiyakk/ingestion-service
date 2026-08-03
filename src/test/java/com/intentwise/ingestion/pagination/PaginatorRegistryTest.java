package com.intentwise.ingestion.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PaginatorRegistry}: lookup by type, unknown type fails clearly. */
class PaginatorRegistryTest {

    private final PaginatorRegistry registry = new PaginatorRegistry(List.of(new SinglePage()));

    @Test
    void getReturnsMatchingPaginator() {
        assertThat(registry.get("single")).isInstanceOf(SinglePage.class);
    }

    @Test
    void getThrowsClearExceptionForUnknownType() {
        assertThatThrownBy(() -> registry.get("offset"))
                .isInstanceOf(PaginationConfigException.class)
                .hasMessageContaining("offset");
    }
}
