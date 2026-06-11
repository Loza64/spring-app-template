package com.server.app.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionDto {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 255, message = "El titulo no puede superar 255 caracteres")
    private String title;

}
