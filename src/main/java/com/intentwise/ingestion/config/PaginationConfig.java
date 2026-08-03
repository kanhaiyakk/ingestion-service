package com.intentwise.ingestion.config;

import jakarta.validation.constraints.NotBlank;

/**
 * Describes how to page through a source's responses. {@code type} selects
 * the {@code Paginator} implementation; the remaining fields are interpreted
 * by that strategy (e.g. page/size query params for offset pagination,
 * {@code cursorPath} for cursor-based pagination). {@code maxPages} is a hard
 * safety cap enforced by the engine regardless of what the API itself
 * reports.
 */
public record PaginationConfig(
        @NotBlank String type,
        String pageParam,
        String sizeParam,
        Integer pageSize,
        Integer startPage,
        String cursorPath,
        Integer maxPages) {
}
