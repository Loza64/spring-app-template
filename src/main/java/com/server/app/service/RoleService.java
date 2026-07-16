package com.server.app.service;

import com.server.app.domain.dto.role.RoleDto;
import com.server.app.domain.model.Role;

public interface RoleService extends ICrudService<Role, Long, RoleDto, RoleDto> {
}
