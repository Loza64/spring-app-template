package com.server.app.domain.dto.response;

public record PaginationMeta(
        int page,
        int pageSize,
        int pageCount,
        long total
) {}
