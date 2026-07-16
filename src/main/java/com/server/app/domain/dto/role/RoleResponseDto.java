package com.server.app.domain.dto.role;

import java.util.Set;

import com.server.app.domain.dto.permission.PermissionResponseDto;

public record RoleResponseDto(Long id, String name, Boolean active, Set<PermissionResponseDto> permissions) {
}
