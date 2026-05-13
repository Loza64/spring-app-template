package com.server.app.controllers;

import com.server.app.dto.response.PaginationMeta;
import com.server.app.dto.user.UserCreateDto;
import com.server.app.dto.user.UserUpdateDto;
import com.server.app.dto.response.Pagination;
import com.server.app.entities.User;
import com.server.app.services.UserService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateDto dto) {
        return ResponseEntity.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable int id, @Valid @RequestBody UserUpdateDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @GetMapping
    public ResponseEntity<Pagination<User>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search
    ) {
        Page<User> p = userService.findAll(page, size, search);
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
    public ResponseEntity<User> findById(@PathVariable int id) {
        return ResponseEntity.ok(userService.findById(id));
    }

}
