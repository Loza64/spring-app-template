package com.server.app.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

import com.server.app.dto.permission.AssingPermissionDto;

@Data
public class RoleDto {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no puede superar 50 caracteres")
    private String name;

    private Set<AssingPermissionDto> permissions;

    private Boolean active;
}