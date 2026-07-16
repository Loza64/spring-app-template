package com.server.app.service;

import com.server.app.domain.dto.permission.PermissionDto;
import com.server.app.domain.model.Permission;

public interface PermissionService extends ICrudService<Permission, Long, PermissionDto, PermissionDto> {
  void createIfNotExists(String path, String method);
}
