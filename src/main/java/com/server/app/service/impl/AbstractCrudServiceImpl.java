package com.server.app.service.impl;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.domain.model.base.SoftDeletableEntity;
import com.server.app.exception.NotFoundException;
import com.server.app.repository.base.SoftDeletableRepository;
import com.server.app.repository.base.SoftDeleteSpecifications;
import com.server.app.service.ICrudService;

public abstract class AbstractCrudServiceImpl<T extends SoftDeletableEntity, ID extends Serializable, CreateDto, UpdateDto>
    implements ICrudService<T, ID, CreateDto, UpdateDto> {

  protected abstract SoftDeletableRepository<T, ID> getRepository();

  protected abstract T mapToEntity(CreateDto dto);

  protected abstract void mapUpdate(UpdateDto dto, T entity);

  protected abstract String entityName();

  @SuppressWarnings("null")
  @Override
  @Transactional
  public T create(CreateDto data) {
    T entity = mapToEntity(data);
    return getRepository().save(entity);
  }

  @SuppressWarnings("null")
  @Override
  @Transactional
  public T update(ID id, UpdateDto data) {
    T entity = findById(id);
    mapUpdate(data, entity);
    return getRepository().save(entity);
  }

  @SuppressWarnings("null")
  @Override
  @Transactional
  public T delete(ID id) {
    T entity = findById(id);
    getRepository().delete(entity);
    return entity;
  }

  @Override
  @Transactional
  public T softDelete(ID id) {
    T entity = findById(id);
    entity.markAsDeleted();
    return getRepository().save(entity);
  }

  @Override
  @Transactional
  public T softRestore(ID id) {
    @SuppressWarnings("null")
    T entity = getRepository().findById(id)
        .orElseThrow(() -> new NotFoundException(entityName() + " no encontrado: " + id));
    entity.restore();
    return getRepository().save(entity);
  }

  @Override
  public T findOneBy(Specification<T> filters) {
    Specification<T> spec = Specification.where(filters).and(SoftDeleteSpecifications.notDeleted());
    return getRepository().findOne(spec)
        .orElseThrow(() -> new NotFoundException(entityName() + " no encontrado"));
  }

  @SuppressWarnings("null")
  @Override
  public T findById(ID id) {
    return getRepository().findById(id)
        .filter(entity -> !entity.isDeleted())
        .orElseThrow(() -> new NotFoundException(entityName() + " no encontrado: " + id));
  }

  @Override
  public Page<T> findBy(Specification<T> filters, Pageable pageable) {
    return findBy(filters, pageable, false);
  }

  @SuppressWarnings("null")
  @Override
  public Page<T> findBy(Specification<T> filters, Pageable pageable, boolean withDeleted) {
    Specification<T> spec = filters == null ? Specification.where(null) : Specification.where(filters);
    if (!withDeleted) {
      spec = spec.and(SoftDeleteSpecifications.notDeleted());
    }
    return getRepository().findAll(spec, pageable);
  }

  @Override
  public long count(Specification<T> filters) {
    Specification<T> spec = (filters == null ? Specification.<T>where(null) : Specification.where(filters))
        .and(SoftDeleteSpecifications.notDeleted());
    return getRepository().count(spec);
  }
}
