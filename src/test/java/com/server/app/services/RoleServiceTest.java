package com.server.app.services;

import com.server.app.dto.permission.AssingPermissionDto;
import com.server.app.dto.role.RoleDto;
import com.server.app.entities.Permission;
import com.server.app.entities.Role;
import com.server.app.exceptions.NotFoundException;
import com.server.app.repositories.PermissionRepository;
import com.server.app.repositories.RoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        readPermission = Permission.builder().id(1L).path("/users").method("GET").build();
        writePermission = Permission.builder().id(2L).path("/users").method("POST").build();
    }

    @Test
    void save_conPermisos_asignaLosPermisosEncontrados() {
        RoleDto dto = RoleDto.builder()
                .name("EDITOR")
                .active(true)
                .permissions(Set.of(
                        permDto(1L),
                        permDto(2L)
                ))
                .build();

        when(permissionRepository.findAllById(anyList()))
                .thenReturn(List.of(readPermission, writePermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role saved = roleService.save(dto);

        assertEquals("EDITOR", saved.getName());
        assertEquals(2, saved.getPermissions().size());
        verify(permissionRepository, times(1)).findAllById(anyList());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void save_sinPermisos_noConsultaPermissionRepository() {
        RoleDto dto = RoleDto.builder()
                .name("VIEWER")
                .active(true)
                .permissions(new HashSet<>())
                .build();

        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role saved = roleService.save(dto);

        assertNull(saved.getPermissions());
        verify(permissionRepository, never()).findAllById(any());
    }

    @Test
    void update_roleExistente_actualizaNombreYPermisos() {
        Role existing = Role.builder().id(5L).name("VIEJO_NOMBRE").active(true)
                .permissions(new HashSet<>()).build();

        RoleDto dto = RoleDto.builder()
                .name("NUEVO_NOMBRE")
                .active(false)
                .permissions(Set.of(permDto(1L)))
                .build();

        when(roleRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of(readPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role updated = roleService.update(5L, dto);

        assertEquals("NUEVO_NOMBRE", updated.getName());
        assertFalse(updated.getActive());
        assertTrue(updated.getPermissions().contains(readPermission));
    }

    @Test
    void update_roleInexistente_lanzaNotFound() {
        RoleDto dto = RoleDto.builder().name("X").build();

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> roleService.update(999L, dto));

        verify(roleRepository, never()).save(any());
    }

    @Test
    void update_sinCampoActive_noModificaElEstadoActual() {
        Role existing = Role.builder().id(7L).name("OLD").active(true)
                .permissions(new HashSet<>()).build();

        RoleDto dto = RoleDto.builder().name("OLD_RENAMED").active(null).build();

        when(roleRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role updated = roleService.update(7L, dto);

        assertTrue(updated.getActive(), "El estado 'active' no debe cambiar si el DTO no lo especifica");
    }

    @Test
    void update_pasaLosIdsCorrectosAFindAllById() {
        Role existing = Role.builder().id(10L).name("X").active(true)
                .permissions(new HashSet<>()).build();
        RoleDto dto = RoleDto.builder()
                .name("X")
                .permissions(Set.of(permDto(1L), permDto(2L)))
                .build();

        when(roleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of(readPermission, writePermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        roleService.update(10L, dto);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionRepository).findAllById(captor.capture());
        assertTrue(captor.getValue().containsAll(List.of(1L, 2L)));
    }

    private AssingPermissionDto permDto(Long id) {
        AssingPermissionDto p = new AssingPermissionDto();
        p.setId(id);
        return p;
    }
}
