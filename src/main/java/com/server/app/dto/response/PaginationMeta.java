package com.server.app.dto.response;

public record PaginationMeta(
        int page,
        int pageSize,
        int pageCount,
        long total
) {}
