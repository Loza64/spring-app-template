package com.server.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.app.domain.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
  Optional<Permission> findByPathAndMethod(String path, String method);
}
