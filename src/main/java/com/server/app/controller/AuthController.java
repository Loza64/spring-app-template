package com.server.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.domain.dto.auth.LoginDto;
import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.response.AuthResponse;
import com.server.app.domain.dto.response.UserResponseDto;
import com.server.app.domain.dto.user.UpdateProfileDto;
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.mapper.UserMapper;
import com.server.app.domain.model.User;
import com.server.app.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserMapper userMapper;

    public AuthController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginDto body) {
        AuthResponse response = userService.login(body.getUsername(), body.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Valid UserCreateDto body) {
        AuthResponse response = userService.signUp(body);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @PutMapping("/update/profile")
    public ResponseEntity<AuthResponse> updateProfile(@RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileDto dto) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        AuthResponse response = userService.updateProfile(token, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/password")
    public ResponseEntity<UserResponseDto> updatePassword(@RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdatePasswordDto dto) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        User user = userService.updatePassword(token, dto);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }
}
