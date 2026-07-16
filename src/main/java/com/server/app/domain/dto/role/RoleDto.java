package com.server.app.domain.dto.role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import com.server.app.domain.dto.permission.AssingPermissionDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no puede superar los 50 caracteres")
    private String name;

    @Valid
    @Builder.Default
    private Set<AssingPermissionDto> permissions = new HashSet<>();

    @Builder.Default
    private Boolean active = true;
}