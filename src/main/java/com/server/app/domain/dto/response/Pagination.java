package com.server.app.domain.dto.response;

import java.util.List;

public record Pagination<T>(
        List<T> data,
        PaginationMeta pagination
) {}