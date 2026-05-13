package com.server.app.services;

import com.server.app.dto.permission.PermissionDto;
import com.server.app.entities.Permission;
import com.server.app.exceptions.NotFoundException;
import com.server.app.repositories.PermissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional
    public Page<Permission> findAll(int page, int size) {
        return permissionRepository.findAll(PageRequest.of(page, size));
    }

    @Transactional
    public Permission findById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found " + id));
        return permission;
    }

    @Transactional
    public void createIfNotExists(String path, String method) {
        Optional<Permission> existing = permissionRepository.findByPathAndMethod(path, method);
        if (existing.isEmpty()) {
            Permission permission = new Permission();
            permission.setPath(path);
            permission.setMethod(method);
            permissionRepository.save(permission);
        }
    }

    @Transactional
    public Permission update(Long id, PermissionDto dto) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found " + id));
        permission.setTitle(dto.getTitle());
        return permissionRepository.save(permission);
    }

}
