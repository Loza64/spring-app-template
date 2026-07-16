package com.server.app.repository;

import java.util.Optional;

import org.springframework.lang.NonNull;

import com.server.app.domain.model.Role;
import com.server.app.repository.base.SoftDeletableRepository;

public interface RoleRepository extends SoftDeletableRepository<Role, Long> {

  @Override
  @NonNull
  Optional<Role> findById(Long id);
}
