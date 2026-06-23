package com.server.app.services;

import com.server.app.config.JsonWebToken;
import com.server.app.dto.auth.UpdatePasswordDto;
import com.server.app.dto.response.AuthResponse;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JsonWebToken jwt;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private Role activeRole;

    @BeforeEach
    void setUp() {
        activeRole = Role.builder()
                .id(2L)
                .name("USER")
                .active(true)
                .build();

        activeUser = User.builder()
                .id(1)
                .username("jperez")
                .name("Juan")
                .surname("Perez")
                .email("jperez@mail.com")
                .password("$2a$encodedHash")
                .blocked(false)
                .role(activeRole)
                .build();
    }

    // ---------------------- LOGIN ----------------------

    @Test
    void login_credencialesValidas_devuelveTokenYUsuario() {
        when(userRepository.findUserByUsername("jperez")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret123", activeUser.getPassword())).thenReturn(true);
        when(jwt.createToken(activeUser)).thenReturn("fake-jwt-token");

        AuthResponse response = userService.login("jperez", "secret123");

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals(activeUser, response.getData());
        verify(jwt, times(1)).createToken(activeUser);
    }

    @Test
    void login_usuarioNoExiste_lanzaUnauthorized() {
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> userService.login("ghost", "whatever"));

        verify(jwt, never()).createToken(any());
    }

    @Test
    void login_usuarioBloqueado_lanzaUnauthorizedYNoGeneraToken() {
        activeUser.setBlocked(true);
        when(userRepository.findUserByUsername("jperez")).thenReturn(Optional.of(activeUser));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.login("jperez", "secret123"));

        assertEquals("Your account has been blocked", ex.getMessage());
        verify(jwt, never()).createToken(any());
    }

    @Test
    void login_passwordIncorrecta_lanzaUnauthorized() {
        when(userRepository.findUserByUsername("jperez")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongpass", activeUser.getPassword())).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> userService.login("jperez", "wrongpass"));

        verify(jwt, never()).createToken(any());
    }

    @Test
    void login_rolDesactivado_lanzaUnauthorized() {
        activeRole.setActive(false);
        when(userRepository.findUserByUsername("jperez")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret123", activeUser.getPassword())).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.login("jperez", "secret123"));

        assertEquals("Your account role has been disabled", ex.getMessage());
        verify(jwt, never()).createToken(any());
    }

    // ---------------------- SIGN UP ----------------------

    @Test
    void signUp_datosValidos_creaUsuarioConRolPorDefecto() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername("nuevo");
        dto.setName("Nuevo");
        dto.setSurname("Usuario");
        dto.setEmail("nuevo@mail.com");
        dto.setPassword("Abcdefg1!");

        when(userRepository.findUserByUsername("nuevo")).thenReturn(Optional.empty());
        when(userRepository.findUserByEmail("nuevo@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Abcdefg1!")).thenReturn("hashed-pass");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(activeRole));
        when(jwt.createToken(any(User.class))).thenReturn("signup-token");

        AuthResponse response = userService.signUp(dto);

        assertEquals("signup-token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void signUp_usernameYaExiste_lanzaConflictYNoGuarda() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername("jperez");
        dto.setEmail("otro@mail.com");

        when(userRepository.findUserByUsername("jperez")).thenReturn(Optional.of(activeUser));

        assertThrows(ConfictException.class, () -> userService.signUp(dto));

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findById(any());
    }

    @Test
    void signUp_rolPorDefectoNoExiste_lanzaNotFound() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername("nuevo");
        dto.setEmail("nuevo@mail.com");
        dto.setPassword("Abcdefg1!");

        when(userRepository.findUserByUsername("nuevo")).thenReturn(Optional.empty());
        when(userRepository.findUserByEmail("nuevo@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(roleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.signUp(dto));
    }

    // ---------------------- UPDATE PASSWORD ----------------------

    @Test
    void updatePassword_datosValidos_actualizaContrasena() {
        String token = "valid-token";
        UpdatePasswordDto dto = new UpdatePasswordDto();
        dto.setOldpassword("oldPass1!");
        dto.setNewpassword("NewPass1!");
        dto.setConfirmpassword("NewPass1!");

        when(jwt.extractIdUser(token)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldPass1!", activeUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", activeUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        userService.updatePassword(token, dto);

        verify(userRepository, times(1)).save(activeUser);
        assertEquals("new-hash", activeUser.getPassword());
    }

    @Test
    void updatePassword_contrasenaActualIncorrecta_lanzaForbidden() {
        String token = "valid-token";
        UpdatePasswordDto dto = new UpdatePasswordDto();
        dto.setOldpassword("wrongOld");
        dto.setNewpassword("NewPass1!");
        dto.setConfirmpassword("NewPass1!");

        when(jwt.extractIdUser(token)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongOld", activeUser.getPassword())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> userService.updatePassword(token, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_nuevaIgualALaAnterior_lanzaBadRequest() {
        String token = "valid-token";
        UpdatePasswordDto dto = new UpdatePasswordDto();
        dto.setOldpassword("samePass1!");
        dto.setNewpassword("samePass1!");
        dto.setConfirmpassword("samePass1!");

        when(jwt.extractIdUser(token)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("samePass1!", activeUser.getPassword())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.updatePassword(token, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_confirmacionNoCoincide_lanzaBadRequest() {
        String token = "valid-token";
        UpdatePasswordDto dto = new UpdatePasswordDto();
        dto.setOldpassword("oldPass1!");
        dto.setNewpassword("NewPass1!");
        dto.setConfirmpassword("Different1!");

        when(jwt.extractIdUser(token)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldPass1!", activeUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", activeUser.getPassword())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.updatePassword(token, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_usuarioBloqueado_lanzaUnauthorized() {
        String token = "valid-token";
        activeUser.setBlocked(true);
        UpdatePasswordDto dto = new UpdatePasswordDto();

        when(jwt.extractIdUser(token)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));

        assertThrows(UnauthorizedException.class, () -> userService.updatePassword(token, dto));

        verify(userRepository, never()).save(any());
    }

    // ---------------------- UPDATE USER ----------------------

    @Test
    void updateUser_actualizaSoloCamposEnviados() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setName("NuevoNombre");
        // username, surname, email, role y blocked se dejan en null a propósito

        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser(1, dto);

        assertEquals("NuevoNombre", updated.getName());
        assertEquals("jperez", updated.getUsername()); // no cambió, dto.username era null
        verify(userRepository, never()).findUserByUsername(any());
        verify(roleRepository, never()).findById(any());
        verify(userRepository, times(1)).save(activeUser);
    }

    @Test
    void updateUser_usuarioBloqueado_lanzaConflict() {
        activeUser.setBlocked(true);
        UserUpdateDto dto = new UserUpdateDto();

        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));

        assertThrows(ConfictException.class, () -> userService.updateUser(1, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_nuevoUsernameYaEnUsoPorOtro_lanzaConflict() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setUsername("ocupado");

        User otroUsuario = User.builder().id(99).username("ocupado").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(userRepository.findUserByUsername("ocupado")).thenReturn(Optional.of(otroUsuario));

        assertThrows(ConfictException.class, () -> userService.updateUser(1, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_rolInexistente_lanzaNotFound() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setRole(50L);

        when(userRepository.findById(1)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateUser(1, dto));

        verify(userRepository, never()).save(any());
    }

    // ---------------------- FIND BY ID ----------------------

    @Test
    void findById_usuarioNoExiste_lanzaNotFound() {
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.findById(404));
    }
}
