package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordDto {
    @NotBlank(message = "La contraseña no puede estar vacía")
    private String oldpassword;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String newpassword;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String confirmpassword;
}
