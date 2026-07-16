package com.server.app.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.config.JsonWebToken;
import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.response.AuthResponse;
import com.server.app.domain.dto.user.UpdateProfileDto;
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.domain.mapper.UserMapper;
import com.server.app.domain.model.Role;
import com.server.app.domain.model.User;
import com.server.app.exception.BadRequestException;
import com.server.app.exception.ConfictException;
import com.server.app.exception.ForbiddenException;
import com.server.app.exception.NotFoundException;
import com.server.app.exception.UnauthorizedException;
import com.server.app.repository.RoleRepository;
import com.server.app.repository.UserRepository;
import com.server.app.repository.base.SoftDeletableRepository;
import com.server.app.service.UserService;

@Service
public class UserServiceImpl extends AbstractCrudServiceImpl<User, Integer, UserCreateDto, UserUpdateDto>
    implements UserService {

  private static final Long DEFAULT_ROLE_ID = 1L;

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final JsonWebToken jwt;
  private final RoleRepository roleRepository;
  private final UserMapper userMapper;

  public UserServiceImpl(PasswordEncoder passwordEncoder, UserRepository userRepository, JsonWebToken jwt,
      RoleRepository roleRepository, UserMapper userMapper) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.jwt = jwt;
    this.roleRepository = roleRepository;
    this.userMapper = userMapper;
  }

  @Override
  protected SoftDeletableRepository<User, Integer> getRepository() {
    return userRepository;
  }

  @Override
  protected User mapToEntity(UserCreateDto dto) {
    uniqueUsername(dto.getUsername(), null);
    uniqueEmail(dto.getEmail(), null);

    User user = userMapper.toEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));

    if (dto.getRole() != null) {
      user.setRole(findRoleOrThrow(dto.getRole()));
    }

    return user;
  }

  @Override
  protected void mapUpdate(UserUpdateDto dto, User entity) {
    if (entity.isBlocked()) {
      throw new ConfictException("The user: " + entity.getUsername() + " is locked");
    }

    if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
      uniqueUsername(dto.getUsername(), entity.getId());
    }

    if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
      uniqueEmail(dto.getEmail(), entity.getId());
    }

    userMapper.updateEntity(dto, entity);

    if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
      entity.setPassword(dto.getPassword());
    }

    if (dto.getRole() != null) {
      entity.setRole(findRoleOrThrow(dto.getRole()));
    }
  }

  @Override
  protected String entityName() {
    return "Usuario";
  }

  @Override
  public AuthResponse login(String username, String password) {
    User user = userRepository.findUserByUsername(username)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    if (user.isBlocked()) {
      throw new UnauthorizedException("Your account has been blocked");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new UnauthorizedException("Contraseña incorrecta");
    }

    if (!user.getRole().getActive()) {
      throw new UnauthorizedException("Your account role has been disabled");
    }

    String token = jwt.createToken(user);
    return new AuthResponse(token, userMapper.toResponseDto(user));
  }

  @Override
  @Transactional
  public AuthResponse signUp(UserCreateDto dto) {
    uniqueUsername(dto.getUsername(), null);
    uniqueEmail(dto.getEmail(), null);

    User user = userMapper.toEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setRole(findRoleOrThrow(DEFAULT_ROLE_ID));

    userRepository.save(user);
    String token = jwt.createToken(user);

    if (!user.getRole().getActive()) {
      throw new UnauthorizedException("Your account role has been disabled");
    }

    return new AuthResponse(token, userMapper.toResponseDto(user));
  }

  @Override
  @Transactional
  public AuthResponse updateProfile(String token, UpdateProfileDto dto) {
    Integer userId = jwt.extractIdUser(token);
    User user = findById(userId);

    if (user.isBlocked()) {
      throw new UnauthorizedException("Your account has been blocked");
    }

    uniqueEmail(dto.getEmail(), userId);
    uniqueUsername(dto.getUsername(), userId);
    userMapper.updateEntity(dto, user);

    User updatedUser = userRepository.save(user);
    return new AuthResponse(token, userMapper.toResponseDto(updatedUser));
  }

  @Override
  @Transactional
  public User updatePassword(String token, UpdatePasswordDto dto) {
    Integer id = jwt.extractIdUser(token);
    User user = findById(id);

    if (user.isBlocked()) {
      throw new UnauthorizedException("Your account is blocked");
    }

    if (!passwordEncoder.matches(dto.getOldpassword(), user.getPassword())) {
      throw new ForbiddenException("La contraseña actual es incorrecta");
    }

    if (passwordEncoder.matches(dto.getNewpassword(), user.getPassword())) {
      throw new BadRequestException("La nueva contraseña no puede ser igual a la anterior");
    }

    if (!dto.getNewpassword().equals(dto.getConfirmpassword())) {
      throw new BadRequestException("Las contraseñas nuevas no coinciden");
    }

    user.setPassword(passwordEncoder.encode(dto.getNewpassword()));
    return userRepository.save(user);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<User> search(int page, int size, String query) {
    return userRepository.search(PageRequest.of(page, size), query == null ? "" : query);
  }

  private Role findRoleOrThrow(Long roleId) {
    return roleRepository.findById(roleId)
        .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + roleId));
  }

  private void uniqueUsername(String username, Integer id) {
    userRepository.findUserByUsername(username).ifPresent(existing -> {
      if (id == null || !existing.getId().equals(id)) {
        throw new ConfictException("El nombre de usuario ya está en uso");
      }
    });
  }

  private void uniqueEmail(String email, Integer id) {
    userRepository.findUserByEmail(email).ifPresent(existing -> {
      if (id == null || !existing.getId().equals(id)) {
        throw new ConfictException("El correo electrónico ya está en uso");
      }
    });
  }
}
