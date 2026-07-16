package com.server.app.domain.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpdateDto {
    @Size(min = 3, max = 20, message = "El nombre de usuario debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\.\\s]+$", message = "El nombre de usuario solo puede contener letras, espacios y puntos")
    private String username;

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    private String name;

    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El apellido solo puede contener letras y espacios")
    private String surname;

    @Email(message = "Debe proporcionar un correo electrónico válido")
    private String email;

    @Positive(message = "El roleId debe ser un número positivo")
    private Long role;

    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$", message = "La contraseña debe incluir al menos una mayúscula, una minúscula, un número y un carácter especial")
    private String password;

    private Boolean blocked;
}
