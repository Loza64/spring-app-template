package com.server.app.controller;

import org.springframework.data.domain.Page;
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
import com.server.app.domain.dto.response.UserResponseDto;
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.domain.mapper.UserMapper;
import com.server.app.domain.model.User;
import com.server.app.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateDto dto) {
        User user = userService.create(dto);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Integer id, @Valid @RequestBody UserUpdateDto dto) {
        User user = userService.update(id, dto);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @GetMapping
    public ResponseEntity<Pagination<UserResponseDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {

        Page<User> result = userService.search(page, size, search);
        return ResponseEntity.ok(PaginationMapper.from(result, userMapper::toResponseDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(userMapper.toResponseDto(userService.findById(id)));
    }

    /** Soft delete: el usuario deja de aparecer en las consultas pero puede restaurarse. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<UserResponseDto> restore(@PathVariable Integer id) {
        User user = userService.softRestore(id);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }
}
