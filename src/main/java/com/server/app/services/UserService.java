package com.server.app.services;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.config.JsonWebToken;
import com.server.app.dto.auth.UpdatePasswordDto;
import com.server.app.dto.response.AuthResponse;
import com.server.app.dto.user.UpdateProfileDto;
import com.server.app.dto.user.UserCreateDto;
import com.server.app.dto.user.UserUpdateDto;
import com.server.app.entities.Role;
import com.server.app.entities.User;
import com.server.app.exceptions.BadRequestException;
import com.server.app.exceptions.ConfictException;
import com.server.app.exceptions.ForbiddenException;
import com.server.app.exceptions.NotFoundException;
import com.server.app.exceptions.UnauthorizedException;
import com.server.app.repositories.RoleRepository;
import com.server.app.repositories.UserRepository;

@Service
@AllArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JsonWebToken jwt;
    private final RoleRepository roleRepository;

    public AuthResponse login(String username, String password) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        if (user.isBlocked()) {
            throw new UnauthorizedException("Your account has been blocked");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Contraseña incorrecta");
        }

        String token = jwt.createToken(user);
        return new AuthResponse(token, user);
    }

    @Transactional
    public AuthResponse signUp(UserCreateDto dto) {
        uniqueUsername(dto.getUsername(), null);
        uniqueEmail(dto.getEmail(), null);
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        Role defaultRole = roleRepository.findById(2L)
                .orElseThrow(() -> new NotFoundException(
                        "Rol por defecto no encontrado, verifica que el rol este registrado en al base de datos"));
        user.setRole(defaultRole);

        userRepository.save(user);
        String token = jwt.createToken(user);

        return new AuthResponse(token, user);
    }

    @Transactional
    public User create(UserCreateDto dto) {
        uniqueUsername(dto.getUsername(), null);
        uniqueEmail(dto.getEmail(), null);
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (dto.getRole() != null) {
            Role role = roleRepository.findById(dto.getRole())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    public Page<User> findAll(int page, int size, String search) {
        return userRepository.findAll(PageRequest.of(page, size), search);
    }

    public User findById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public AuthResponse updateProfile(String token, UpdateProfileDto dto) {
        int userId = jwt.extractIdUser(token);
        User user = findById(userId);

        if (user.isBlocked()) {
            throw new UnauthorizedException("Your account has been blocked");
        }

        uniqueEmail(dto.getEmail(), userId);
        uniqueUsername(dto.getUsername(), userId);
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        User updatedUser = userRepository.save(user);
        return new AuthResponse(token, updatedUser);
    }

    @Transactional
    public User updatePassword(String token, UpdatePasswordDto dto) {
        int id = jwt.extractIdUser(token);
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

    @Transactional
    public User updateUser(int userId, UserUpdateDto dto) {
        User user = findById(userId);

        if (user.isBlocked()) {
            throw new ConfictException("The user: " + user.getUsername() + " is locked");
        }

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            uniqueUsername(dto.getUsername(), userId);
            user.setUsername(dto.getUsername());
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }

        if (dto.getSurname() != null && !dto.getSurname().isBlank()) {
            user.setSurname(dto.getSurname());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            uniqueEmail(dto.getEmail(), userId);
            user.setEmail(dto.getEmail());
        }

        if (dto.getBlocked() != null) {
            user.setBlocked(dto.getBlocked());
        }

        if (dto.getRole() != null) {
            Role role = roleRepository.findById(dto.getRole())
                    .orElseThrow(() -> new NotFoundException("Rol no encontrado"));
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    private void uniqueUsername(String username, Integer id) {
        userRepository.findUserByUsername(username).ifPresent(existing -> {
            if (id == null || existing.getId() != id) {
                throw new ConfictException("El nombre de usuario ya está en uso");
            }
        });
    }

    private void uniqueEmail(String email, Integer id) {
        userRepository.findUserByEmail(email).ifPresent(existing -> {
            if (id == null || existing.getId() != id) {
                throw new ConfictException("El correo electrónico ya está en uso");
            }
        });
    }
}
