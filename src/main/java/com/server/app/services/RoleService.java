package com.server.app.services;

import com.server.app.dto.permission.AssingPermissionDto;
import com.server.app.dto.role.RoleDto;
import com.server.app.entities.Permission;
import com.server.app.entities.Role;
import com.server.app.exceptions.NotFoundException;
import com.server.app.repositories.PermissionRepository;
import com.server.app.repositories.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public Page<Role> findAll(int page, int size) {
        return roleRepository.findAll(PageRequest.of(page, size));
    }

    @Transactional
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Transactional
    public Role save(RoleDto dto) {
        Role role = new Role();
        role.setName(dto.getName());
        if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
            List<Long> ids = dto.getPermissions()
                    .stream()
                    .map(AssingPermissionDto::getId)
                    .filter(Objects::nonNull)
                    .toList();
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(permissions);
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role update(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role not found"));
        role.setName(dto.getName());
        if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
            List<Long> ids = dto.getPermissions()
                    .stream()
                    .map(AssingPermissionDto::getId)
                    .filter(Objects::nonNull)
                    .toList();
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(permissions);
        }
        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

}
