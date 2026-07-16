package com.server.app.domain.dto.auth;

import com.server.app.domain.dto.user.UserResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {
  private String token;
  private UserResponseDto data;
}
