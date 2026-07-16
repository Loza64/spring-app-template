package com.server.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.common.exceptions.BadRequestException;
import com.server.app.common.exceptions.ConfictException;
import com.server.app.common.exceptions.ForbiddenException;
import com.server.app.common.exceptions.NotFoundException;
import com.server.app.common.exceptions.UnauthorizedException;
import com.server.app.config.JsonWebTokenProvider;
import com.server.app.domain.dto.auth.AuthResponseDto;
import com.server.app.domain.dto.auth.LoginDto;
import com.server.app.domain.dto.auth.SignUpDto;
import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.auth.UpdateProfileDto;
import com.server.app.domain.dto.user.UserResponseDto;
import com.server.app.domain.mapper.AuthMapper;
import com.server.app.domain.mapper.UserMapper;
import com.server.app.domain.model.Role;
import com.server.app.domain.model.User;
import com.server.app.repository.RoleRepository;
import com.server.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final AuthMapper authMapper;
  private final UserMapper userMapper;
  private final JsonWebTokenProvider jwt;

  private static final Long DEFAULT_ROLE_ID = 1L;

  @Transactional(readOnly = true)
  public AuthResponseDto login(LoginDto login) {
    User profile = userRepository.findByUsername(login.username())
        .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

    if (!passwordEncoder.matches(login.password(), profile.getPassword())) {
      throw new ForbiddenException("La contraseña es incorrecta");
    }

    UserResponseDto response = userMapper.toResponseDto(profile);
    return new AuthResponseDto(jwt.createToken(response), response);
  }

  @Transactional
  public AuthResponseDto signup(SignUpDto dto) {
    if (userRepository.existsByEmail(dto.email()))
      throw new ConfictException("El email ya está registrado");
    if (userRepository.existsByUsername(dto.username()))
      throw new ConfictException("El nombre de usuario ya está ocupado");

    User data = authMapper.toSignUp(dto);
    data.setPassword(passwordEncoder.encode(dto.password()));

    Role defaultRole = roleRepository.findById(DEFAULT_ROLE_ID)
        .orElseThrow(() -> new NotFoundException("Rol por defecto no configurado"));
    data.setRole(defaultRole);

    User savedUser = userRepository.save(data);
    UserResponseDto response = userMapper.toResponseDto(savedUser);
    return new AuthResponseDto(jwt.createToken(response), response);
  }

  @Transactional
  public UserResponseDto updateProfile(String token, UpdateProfileDto dto) {
    Long id = jwt.extractIdUser(token);
    User profile = validateAndGetUser(id);

    authMapper.updateEntity(dto, profile);
    return userMapper.toResponseDto(userRepository.save(profile));
  }

  @Transactional
  public UserResponseDto updatePassword(String token, UpdatePasswordDto dto) {
    User profile = validateAndGetUser(jwt.extractIdUser(token));

    if (!passwordEncoder.matches(dto.currentPassword(), profile.getPassword()))
      throw new ForbiddenException("La contraseña actual es incorrecta");

    if (!dto.newPassword().equals(dto.confirmNewPassword()))
      throw new BadRequestException("Las contraseñas nuevas no coinciden");

    if (passwordEncoder.matches(dto.newPassword(), profile.getPassword()))
      throw new BadRequestException("La nueva contraseña no puede ser igual a la anterior");

    profile.setPassword(passwordEncoder.encode(dto.newPassword()));
    return userMapper.toResponseDto(userRepository.save(profile));
  }

  private User validateAndGetUser(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

    if (user.isBlocked())
      throw new UnauthorizedException("Cuenta bloqueada");
    if (user.getDeletedAt() != null)
      throw new UnauthorizedException("Cuenta eliminada");

    return user;
  }
}