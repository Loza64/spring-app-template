package com.server.app.services;

import com.server.app.dto.permission.PermissionDto;
import com.server.app.entities.Permission;
import com.server.app.exceptions.NotFoundException;
import com.server.app.repositories.PermissionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void createIfNotExists_permisoNoExiste_loCreaConPathYMethod() {
        when(permissionRepository.findByPathAndMethod("/users", "GET"))
                .thenReturn(Optional.empty());

        permissionService.createIfNotExists("/users", "GET");

        verify(permissionRepository, times(1)).save(argThat(permission ->
                permission.getPath().equals("/users") && permission.getMethod().equals("GET")
        ));
    }

    @Test
    void createIfNotExists_permisoYaExiste_noLoVuelveACrear() {
        Permission existente = Permission.builder().id(1L).path("/users").method("GET").build();
        when(permissionRepository.findByPathAndMethod("/users", "GET"))
                .thenReturn(Optional.of(existente));

        permissionService.createIfNotExists("/users", "GET");

        verify(permissionRepository, never()).save(any());
    }

    @Test
    void findById_permisoExiste_loRetorna() {
        Permission permission = Permission.builder().id(3L).path("/roles").method("PUT").build();
        when(permissionRepository.findById(3L)).thenReturn(Optional.of(permission));

        Permission result = permissionService.findById(3L);

        assertEquals("/roles", result.getPath());
        assertEquals("PUT", result.getMethod());
    }

    @Test
    void findById_permisoNoExiste_lanzaNotFound() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> permissionService.findById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void update_permisoExiste_actualizaTitulo() {
        Permission existing = Permission.builder().id(4L).path("/roles").method("DELETE").title("old").build();
        PermissionDto dto = new PermissionDto();
        dto.setTitle("Eliminar rol");

        when(permissionRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission updated = permissionService.update(4L, dto);

        assertEquals("Eliminar rol", updated.getTitle());
        verify(permissionRepository, times(1)).save(existing);
    }

    @Test
    void update_permisoNoExiste_lanzaNotFoundYNoGuarda() {
        PermissionDto dto = new PermissionDto();
        dto.setTitle("Nuevo titulo");

        when(permissionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> permissionService.update(100L, dto));

        verify(permissionRepository, never()).save(any());
    }
}
