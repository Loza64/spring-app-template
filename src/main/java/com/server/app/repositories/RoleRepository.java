package com.server.app.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.server.app.entities.Role;

import java.util.Optional;

@EnableJpaRepositories
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Override
    Optional<Role> findById(Long aLong);

    Page<Role> findAll(Pageable pageable);
}
