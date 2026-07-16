package com.server.app.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Integer id;
    private String username;
    private String name;
    private String surname;
    private String email;
    private boolean blocked;
    private Long roleId;
    private String roleName;
}
