package com.server.app.domain.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public final class PaginationMapper {

    private PaginationMapper() {
    }

    public static <T> Pagination<T> from(Page<T> page) {
        return new Pagination<>(page.getContent(), toMeta(page));
    }

    public static <T, R> Pagination<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> data = page.getContent().stream().map(mapper).toList();
        return new Pagination<>(data, toMeta(page));
    }

    private static PaginationMeta toMeta(Page<?> page) {
        return new PaginationMeta(page.getNumber(), page.getSize(), page.getTotalPages(), page.getTotalElements());
    }
}
