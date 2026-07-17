package com.server.app.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionMapper permissionMapper;
  private final PaginationMapper paginationMapper;

  @Transactional
  public void createAllIfNotExists(List<Permission> candidates) {
    if (candidates.isEmpty())
      return;

    Set<String> existing = permissionRepository.findAll().stream()
        .map(p -> key(p.getPath(), p.getMethod()))
        .collect(Collectors.toSet());

    List<Permission> toCreate = candidates.stream()
        .filter(p -> !existing.contains(key(p.getPath(), p.getMethod())))
        .toList();

    if (!toCreate.isEmpty()) {
      permissionRepository.saveAll(toCreate);
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

  private String key(String path, String method) {
    return method + ":" + path;
  }
}