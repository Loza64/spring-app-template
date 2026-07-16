package com.server.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.domain.dto.permission.PermissionDto;
import com.server.app.domain.dto.response.Pagination;
import com.server.app.domain.dto.response.PaginationMapper;
import com.server.app.domain.model.Permission;
import com.server.app.service.PermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GetMapping
  public ResponseEntity<Pagination<Permission>> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<Permission> result = permissionService.findBy(Specification.where(null), PageRequest.of(page, size));
    return ResponseEntity.ok(PaginationMapper.from(result));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Permission> findById(@PathVariable Long id) {
    return ResponseEntity.ok(permissionService.findById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Permission> update(@PathVariable Long id, @Valid @RequestBody PermissionDto dto) {
    return ResponseEntity.ok(permissionService.update(id, dto));
  }

  /** Permission no soporta soft delete: esta operacion es un hard delete real. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    permissionService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
