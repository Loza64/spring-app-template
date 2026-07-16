package com.server.app.service;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ICrudService<T, ID extends Serializable, CreateDto, UpdateDto> {

  T create(CreateDto data);

  T update(ID id, UpdateDto data);

  T delete(ID id);

  T softDelete(ID id);

  T softRestore(ID id);

  T findOneBy(Specification<T> filters);

  T findById(ID id);

  Page<T> findBy(Specification<T> filters, Pageable pageable);

  Page<T> findBy(Specification<T> filters, Pageable pageable, boolean withDeleted);

  long count(Specification<T> filters);
}
