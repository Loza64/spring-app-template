package com.server.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.domain.dto.auth.AuthResponseDto;
import com.server.app.domain.dto.auth.LoginDto;
import com.server.app.domain.dto.auth.SignUpDto;
import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.auth.UpdateProfileDto;
import com.server.app.domain.dto.user.UserResponseDto;
import com.server.app.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto dto) {
    return ResponseEntity.ok(authService.login(dto));
  }

  @PostMapping("/signup")
  public ResponseEntity<AuthResponseDto> signup(@Valid @RequestBody SignUpDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(dto));
  }

  @GetMapping("/profile")
  public ResponseEntity<UserResponseDto> getProfile(@AuthenticationPrincipal UserResponseDto user) {
    return ResponseEntity.ok(user);
  }

  @PutMapping("/profile")
  public ResponseEntity<UserResponseDto> updateProfile(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody UpdateProfileDto dto) {
    String token = authorization.replace("Bearer ", "");
    return ResponseEntity.ok(authService.updateProfile(token, dto));
  }

  @PatchMapping("/password")
  public ResponseEntity<UserResponseDto> updatePassword(
      @RequestHeader("Authorization") String authorization,
      @Valid @RequestBody UpdatePasswordDto dto) {
    String token = authorization.replace("Bearer ", "");
    return ResponseEntity.ok(authService.updatePassword(token, dto));
  }
}
