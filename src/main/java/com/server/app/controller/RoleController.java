package com.server.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.domain.dto.response.Pagination;
import com.server.app.domain.dto.response.PaginationMapper;
import com.server.app.domain.dto.role.RoleDto;
import com.server.app.domain.model.Role;
import com.server.app.service.RoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @PostMapping
  public ResponseEntity<Role> save(@Valid @RequestBody RoleDto role) {
    return ResponseEntity.ok(roleService.create(role));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Role> update(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
    return ResponseEntity.ok(roleService.update(id, dto));
  }

  @GetMapping
  public ResponseEntity<Pagination<Role>> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<Role> result = roleService.findBy(Specification.where(null), PageRequest.of(page, size));
    return ResponseEntity.ok(PaginationMapper.from(result));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Role> findById(@PathVariable Long id) {
    return ResponseEntity.ok(roleService.findById(id));
  }

  /**
   * Soft delete: el rol deja de aparecer en las consultas pero puede restaurarse.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    roleService.softDelete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/restore")
  public ResponseEntity<Role> restore(@PathVariable Long id) {
    return ResponseEntity.ok(roleService.softRestore(id));
  }
}
