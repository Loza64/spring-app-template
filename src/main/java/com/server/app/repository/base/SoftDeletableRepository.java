package com.server.app.repository.base;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import com.server.app.domain.model.base.SoftDeletableEntity;

/**
 * Repositorio base para entidades que soportan soft delete. Combina
 * {@link JpaRepository} (operaciones CRUD basicas) con
 * {@link JpaSpecificationExecutor} (consultas dinamicas mediante
 * {@code Specification}, equivalente JPA de {@code FindOptionsWhere}).
 */
@NoRepositoryBean
public interface SoftDeletableRepository<T extends SoftDeletableEntity, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}
