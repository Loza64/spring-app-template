package com.server.app.domain.dto.user;

import com.server.app.domain.dto.role.RoleResponseDto;

public record UserResponseDto(
    Long id,
    String username,
    String name,
    String surname,
    String email,
    boolean blocked,
    RoleResponseDto role) {
}