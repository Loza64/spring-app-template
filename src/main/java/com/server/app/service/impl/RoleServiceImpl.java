package com.server.app.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.app.domain.dto.permission.AssingPermissionDto;
import com.server.app.domain.dto.role.RoleDto;
import com.server.app.domain.mapper.RoleMapper;
import com.server.app.domain.model.Permission;
import com.server.app.domain.model.Role;
import com.server.app.repository.PermissionRepository;
import com.server.app.repository.RoleRepository;
import com.server.app.repository.base.SoftDeletableRepository;
import com.server.app.service.RoleService;

@Service
public class RoleServiceImpl extends AbstractCrudServiceImpl<Role, Long, RoleDto, RoleDto> implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository,
            RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    protected SoftDeletableRepository<Role, Long> getRepository() {
        return roleRepository;
    }

    @Override
    protected Role mapToEntity(RoleDto dto) {
        Role role = roleMapper.toEntity(dto);
        role.setPermissions(resolvePermissions(dto.getPermissions()));
        return role;
    }

    @Override
    protected void mapUpdate(RoleDto dto, Role entity) {
        roleMapper.updateEntity(dto, entity);
        if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
            entity.setPermissions(resolvePermissions(dto.getPermissions()));
        }
    }

    @Override
    protected String entityName() {
        return "Role";
    }

    /** Resuelve el set de {@code AssingPermissionDto} contra el catalogo real de permisos. */
    private Set<Permission> resolvePermissions(Set<AssingPermissionDto> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> ids = permissions.stream()
                .filter(Objects::nonNull)
                .map(AssingPermissionDto::getId)
                .filter(Objects::nonNull)
                .toList();

        return new HashSet<>(permissionRepository.findAllById(ids));
    }
}
