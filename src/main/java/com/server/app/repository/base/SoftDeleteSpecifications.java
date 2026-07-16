package com.server.app.repository.base;

import org.springframework.data.jpa.domain.Specification;

import com.server.app.domain.model.base.SoftDeletableEntity;

public final class SoftDeleteSpecifications {

    private SoftDeleteSpecifications() {
    }

    /** Predicado reutilizable: {@code deleted_at IS NULL}. */
    public static <T extends SoftDeletableEntity> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /** Predicado reutilizable: {@code deleted_at IS NOT NULL}. */
    public static <T extends SoftDeletableEntity> Specification<T> onlyDeleted() {
        return (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
    }
}
