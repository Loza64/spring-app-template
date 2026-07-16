package com.server.app.service;

import com.server.app.common.pagination.PaginationMapper;
import com.server.app.common.pagination.PaginationResponse;
import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.domain.mapper.PermissionMapper;
import com.server.app.domain.model.Permission;
import com.server.app.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.app.common.exceptions.NotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionMapper permissionMapper;
  private final PaginationMapper paginationMapper;

  @Transactional
  public void createIfNotExists(String path, String method) {
    Optional<Permission> existing = permissionRepository.findByPathAndMethod(path, method);
    if (existing.isEmpty()) {
      Permission permission = Permission.builder()
          .path(path)
          .method(method)
          .build();
      permissionRepository.save(permission);
    }
  }

  @Transactional
  public PermissionResponseDto update(Long id, PermissionUpdateDto dto) {
    Permission permission = permissionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Permiso no encontrado con ID: " + id));

    permissionMapper.updateEntity(dto, permission);
    return permissionMapper.toResponseDto(permissionRepository.save(permission));
  }

  @Transactional(readOnly = true)
  public PaginationResponse<PermissionResponseDto> findAll(Pageable pageable) {
    Page<Permission> page = permissionRepository.findAll(pageable);
    return paginationMapper.toPaginationResponse(page.map(permissionMapper::toResponseDto));
  }
}