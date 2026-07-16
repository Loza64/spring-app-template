package com.server.app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.common.pagination.PaginationResponse;
import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.service.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

  private final PermissionService permissionService;

  @GetMapping
  public ResponseEntity<PaginationResponse<PermissionResponseDto>> findAll(Pageable pageable) {
    return ResponseEntity.ok(permissionService.findAll(pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PermissionResponseDto> update(
      @PathVariable Long id,
      @Valid @RequestBody PermissionUpdateDto dto) {
    return ResponseEntity.ok(permissionService.update(id, dto));
  }
}
