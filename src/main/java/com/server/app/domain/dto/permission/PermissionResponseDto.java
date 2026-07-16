package com.server.app.domain.dto.permission;

public record PermissionResponseDto(Long id, String path, String method, String title) {
}