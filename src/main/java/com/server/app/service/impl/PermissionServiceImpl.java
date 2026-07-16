package com.server.app.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.domain.dto.permission.PermissionDto;
import com.server.app.domain.mapper.PermissionMapper;
import com.server.app.domain.model.Permission;
import com.server.app.exception.NotFoundException;
import com.server.app.repository.PermissionRepository;
import com.server.app.service.PermissionService;

/**
 * Permission es un catalogo de solo consulta generado automaticamente a
 * partir de los endpoints expuestos; por eso no participa del ciclo de soft
 * delete y no extiende {@link AbstractCrudServiceImpl}.
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    @Transactional
    public Permission create(PermissionDto data) {
        Permission permission = permissionMapper.toEntity(data);
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Permission update(Long id, PermissionDto data) {
        Permission permission = findById(id);
        permissionMapper.updateEntity(data, permission);
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Permission delete(Long id) {
        Permission permission = findById(id);
        permissionRepository.delete(permission);
        return permission;
    }

    /**
     * Permission no tiene columna {@code deletedAt}: aqui "soft delete"
     * degrada de forma explicita a un hard delete real.
     */
    @Override
    @Transactional
    public Permission softDelete(Long id) {
        return delete(id);
    }

    @Override
    public Permission softRestore(Long id) {
        throw new UnsupportedOperationException(
                "Permission no soporta soft delete/restore: los permisos eliminados se eliminan de forma definitiva");
    }

    @Override
    public Permission findOneBy(Specification<Permission> filters) {
        return permissionRepository.findOne(filters)
                .orElseThrow(() -> new NotFoundException("Permission no encontrado"));
    }

    @Override
    public Permission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission no encontrado: " + id));
    }

    @Override
    public Page<Permission> findBy(Specification<Permission> filters, Pageable pageable) {
        return permissionRepository.findAll(filters, pageable);
    }

    @Override
    public Page<Permission> findBy(Specification<Permission> filters, Pageable pageable, boolean withDeleted) {
        // withDeleted no aplica: Permission no soporta soft delete.
        return findBy(filters, pageable);
    }

    @Override
    public long count(Specification<Permission> filters) {
        return permissionRepository.count(filters);
    }

    @Override
    @Transactional
    public void createIfNotExists(String path, String method) {
        Optional<Permission> existing = permissionRepository.findByPathAndMethod(path, method);
        if (existing.isEmpty()) {
            Permission permission = Permission.builder()
                    .path(path)
                    .method(method)
                    .build();
            permissionRepository.save(permission);
        }
    }
}
