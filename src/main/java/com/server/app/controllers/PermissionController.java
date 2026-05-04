package com.server.app.controllers;

import com.server.app.dto.permission.PermissionDto;
import com.server.app.dto.response.Pagination;
import com.server.app.dto.response.PaginationMeta;
import com.server.app.entities.Permission;
import com.server.app.services.PermissionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

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

        var p = permissionService.findAll(page, size);

        return ResponseEntity.ok(new Pagination<>(
                p.getContent(),
                new PaginationMeta(
                        p.getNumber(),
                        p.getSize(),
                        p.getTotalPages(),
                        p.getTotalElements()
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> findById(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permission> update(@PathVariable Long id, @Valid @RequestBody PermissionDto dto) {
        return ResponseEntity.ok(permissionService.update(id, dto));
    }

}
